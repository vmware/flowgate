/*
See LICENSE folder for this sample’s licensing information.

Abstract:
SceneKit shader modifier to render bounding box edges with distance-based fade.
*/
#pragma transparent
#pragma body

float3 modelPosition = scn_node.modelTransform[3].xyz;
float3 viewPosition = scn_frame.inverseViewTransform[3].xyz;
float distance = length(modelPosition - viewPosition);

float3 bBoxMin = scn_node.boundingBox[0];
float3 bBoxMax = scn_node.boundingBox[1];
float3 size = bBoxMax - bBoxMin;
float bBoxDiag = length(size);

////////////////////////////////////////////////////////////////
// Compute per-pixel transparency based on distance from camera
////////////////////////////////////////////////////////////////
float closest = distance - bBoxDiag / 2.0;
float furthest = distance + bBoxDiag / 2.0;
float distFromPointOfView = length(_surface.position);
float normalizedDistance = 1 - ((distFromPointOfView - closest) / (furthest - closest));
_surface.transparent.a = clamp(normalizedDistance, 0.0, 1.0);

////////////////////////////////////////////////////////////////
// Render only a wireframe
////////////////////////////////////////////////////////////////
float lineThickness = 0.002;
float u = _surface.diffuseTexcoord.x;
float v = _surface.diffuseTexcoord.y;

// Compute scaling of line thickness based on bounding box size
float2 scale;
if (abs((scn_node.inverseModelViewTransform * float4(_surface.normal, 0.0)).x) > 0.5) {
    scale = size.zy;
} else if (abs((scn_node.inverseModelViewTransform * float4(_surface.normal, 0.0)).y) > 0.5) {
    scale = size.xz;
} else {
    scale = size.xy;
}

// Compute threshold for discarding rendering
float2 thresh = float2(lineThickness) / scale;
if (u > thresh[0] && u < (1.0 - thresh[0]) && v > thresh[1] && v < (1.0 - thresh[1])) {
    discard_fragment();
}
