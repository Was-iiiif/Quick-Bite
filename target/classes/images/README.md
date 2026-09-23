# Image Assets

All PNG (and other image) files referenced by the `image_url` column of the `restaurants` table should be placed in this directory.

The application loads images using:
```java
new Image(getClass().getResourceAsStream("/images/" + imageFileName))
```
Therefore the folder `src/main/resources/images/` becomes part of the classpath at runtime, and any files here will be available via the `/images/` path.

**Steps to add a new restaurant image**:
1. Copy the PNG file (e.g., `pizza.png`) into this folder.
2. Ensure the filename matches the value stored in the `image_url` column.
3. Re‑build the project (`compile.ps1`) so the resource is packaged.

If the folder does not exist yet, creating this README will also create the necessary directory structure.
