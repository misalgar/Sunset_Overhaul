# R8 is disabled in the foundation scaffold (see app/build.gradle.kts).
# Before enabling minification:
#   - keep kotlinx.serialization models used by the future provider DTOs
#   - keep Room generated implementations (Room ships consumer rules)
#   - keep Media3 / Hilt consumer rules (both ship with their libraries)
#   - add rules for any future cast/download SDK, then run a release build with mapping upload.
