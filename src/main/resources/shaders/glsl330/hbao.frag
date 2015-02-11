// $shader_type: fragment

// $texture_layout: depths = 0
// $texture_layout: noise = 1

#version 330

const float PI = 3.14159265;

uniform sampler2D depths;
uniform sampler2D noise;

in vec2 textureUV;
noperspective in vec3 viewRay;

uniform vec2 projection;
uniform float tanHalfFOV;
uniform float aspectRatio;

uniform vec2 AORes = vec2(640, 480);
uniform vec2 InvAORes = vec2(1.0 / 640, 1.0 / 480);
uniform vec2 NoiseScale = vec2(640, 480) / 4;

uniform float AOStrength = 1.9;
uniform float R = 0.3;
uniform float R2 = 0.3 * 0.3;
uniform float NegInvR2 = -1 / (0.3 * 0.3);
uniform float TanBias = tan(30 * PI / 180);
uniform float MaxRadiusPixels = 100;

uniform int NumDirections = 6;
uniform int NumSamples = 4;

out float out_frag0;

float linearizeDepth(float depth) {
    return projection.y / (depth - projection.x);
}

vec3 GetViewPos(vec2 uv)
{
    float depth = linearizeDepth(texture(depths, uv).r);
    return viewRay * depth;
}

vec3 GetViewPosPoint(ivec2 uv)
{
    ivec2 coord = ivec2(gl_FragCoord.xy) + uv;
    float depth = linearizeDepth(texelFetch(depths, coord, 0).r);
    return viewRay * depth;
}

float TanToSin(float x)
{
	return x * inversesqrt(x * x + 1);
}

float InvLength(vec2 V)
{
	return inversesqrt(dot(V, V));
}

float Tangent(vec3 V)
{
	return V.z * InvLength(V.xy);
}

float BiasedTangent(vec3 V)
{
	return V.z * InvLength(V.xy) + TanBias;
}

float Tangent(vec3 P, vec3 S)
{
    return -(P.z - S.z) * InvLength(S.xy - P.xy);
}

float Length2(vec3 V)
{
	return dot(V, V);
}

vec3 MinDiff(vec3 P, vec3 Pr, vec3 Pl)
{
    vec3 V1 = Pr - P;
    vec3 V2 = P - Pl;
    return (Length2(V1) < Length2(V2)) ? V1 : V2;
}

vec2 SnapUVOffset(vec2 uv)
{
    return round(uv * AORes) * InvAORes;
}

float Falloff(float d2)
{
	return d2 * NegInvR2 + 1;
}

float HorizonOcclusion(	vec2 deltaUV,
						vec3 P,
						vec3 dPdu,
						vec3 dPdv,
						float randstep,
						float numSamples)
{
	float ao = 0;

	// Offset the first coord with some noise
	vec2 uv = textureUV + SnapUVOffset(randstep * deltaUV);
	deltaUV = SnapUVOffset(deltaUV);

	// Calculate the tangent vector
	vec3 T = deltaUV.x * dPdu + deltaUV.y * dPdv;

	// Get the angle of the tangent vector from the viewspace axis
	float tanH = BiasedTangent(T);
	float sinH = TanToSin(tanH);

	float tanS;
	float d2;
	vec3 S;

	// Sample to find the maximum angle
	for(float s = 1; s <= numSamples; s++)
	{
		uv += deltaUV;
		S = GetViewPos(uv);
		tanS = Tangent(P, S);
		d2 = Length2(S - P);

		// Is the sample within the radius and the angle greater?
		if(d2 < R2 && tanS > tanH)
		{
			float sinS = TanToSin(tanS);
			// Apply falloff based on the distance
			ao += Falloff(d2) * (sinS - sinH);

			tanH = tanS;
			sinH = sinS;
		}
	}

	return ao;
}

vec2 RotateDirections(vec2 Dir, vec2 CosSin)
{
    return vec2(Dir.x * CosSin.x - Dir.y * CosSin.y, Dir.x * CosSin.y + Dir.y * CosSin.x);
}

void ComputeSteps(inout vec2 stepSizeUv, inout float numSteps, float rayRadiusPix, float rand)
{
    // Avoid oversampling if numSteps is greater than the kernel radius in pixels
    numSteps = min(NumSamples, rayRadiusPix);

    // Divide by Ns+1 so that the farthest samples are not fully attenuated
    float stepSizePix = rayRadiusPix / (numSteps + 1);

    // Clamp numSteps if it is greater than the max kernel footprint
    float maxNumSteps = MaxRadiusPixels / stepSizePix;
    if (maxNumSteps < numSteps)
    {
        // Use dithering to avoid AO discontinuities
        numSteps = floor(maxNumSteps + rand);
        numSteps = max(numSteps, 1);
        stepSizePix = MaxRadiusPixels / numSteps;
    }

    // Step size in uv space
    stepSizeUv = stepSizePix * InvAORes;
}

void main(void)
{
	float numDirections = NumDirections;

	vec3 P, Pr, Pl, Pt, Pb;
	P = GetViewPos(textureUV);

	// Sample neighboring pixels
    Pr = GetViewPos(textureUV + vec2( InvAORes.x, 0));
    Pl = GetViewPos(textureUV + vec2(-InvAORes.x, 0));
    Pt = GetViewPos(textureUV + vec2( 0, InvAORes.y));
    Pb = GetViewPos(textureUV + vec2( 0,-InvAORes.y));

    // Calculate tangent basis vectors using the minimum difference
    vec3 dPdu = MinDiff(P, Pr, Pl);
    vec3 dPdv = MinDiff(P, Pt, Pb) * (AORes.y * InvAORes.x);

    // Get the random samples from the noise texture
	vec3 random = texture(noise, textureUV.xy * NoiseScale).rgb;

	// Calculate the projected size of the hemisphere
	vec2 FocalLen = vec2(1.0 / tanHalfFOV * aspectRatio, 1.0 / tanHalfFOV);
    vec2 rayRadiusUV = 0.5 * R * FocalLen / -P.z;
    float rayRadiusPix = rayRadiusUV.x * AORes.x;

    float ao = 1;

    // Make sure the radius of the evaluated hemisphere is more than a pixel
    if(rayRadiusPix > 1)
    {
    	ao = 0;
    	float numSteps;
    	vec2 stepSizeUV;

    	// Compute the number of steps
    	ComputeSteps(stepSizeUV, numSteps, rayRadiusPix, random.z);

		float alpha = 2 * PI / numDirections;

		// Calculate the horizon occlusion of each direction
		for(float d = 0; d < numDirections; ++d)
		{
			float theta = alpha * d;

			// Apply noise to the direction
			vec2 dir = RotateDirections(vec2(cos(theta), sin(theta)), random.xy);
			vec2 deltaUV = dir * stepSizeUV;

			// Sample the pixels along the direction
			ao += HorizonOcclusion(	deltaUV,
									P,
									dPdu,
									dPdv,
									random.z,
									numSteps);
		}

		// Average the results and produce the final AO
		ao = 1 - ao / numDirections * AOStrength;
	}

	out_frag0 = ao;
}
