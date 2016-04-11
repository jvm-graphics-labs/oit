package demos.dualDepthPeeling;

// Translated from C++ Version:
// nvModel.h - Model support class
//
// The nvModel class implements an interface for a multipurpose model
// object. This class is useful for loading and formatting meshes
// for use by OpenGL. It can compute face normals, tangents, and
// adjacency information. The class supports the obj file format.
//
// Author: Evan Hart
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
////////////////////////////////////////////////////////////////////////////////
import static com.jogamp.opengl.GL.*;
import com.jogamp.opengl.GL2;
import static com.jogamp.opengl.GLES2.*;
import static com.jogamp.opengl.fixedfunc.GLPointerFunc.GL_NORMAL_ARRAY;
import com.jogamp.opengl.util.GLBuffers;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public final class Model {

    private ArrayList<Float> positions = new ArrayList<>();
    private ArrayList<Float> normals = new ArrayList<>();
    private int posSize;

    private ArrayList<Integer> pIndex = new ArrayList<>();
    private ArrayList<Integer> nIndex = new ArrayList<>();

    //data structures optimized for rendering, compiled model
    private IntBuffer indices = null;
    private FloatBuffer vertices = null;
    private int pOffset;
    private int nOffset;
    private int vtxSize = 0;

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int MAX = 2;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX), vertexArrayName = GLBuffers.newDirectIntBuffer(1);
    public static float scale;
    public static float[] trans;

    public Model(GL2 gl2, String filename) {

        posSize = 0;
        pOffset = -1;
        nOffset = -1;
        vtxSize = 0;

        System.err.println("loading OBJ...\n");
        loadModelFromFile(filename);

        System.err.println("compiling mesh...\n");
        compileModel();

        System.err.println(getPositionCount() + " vertices");
        System.err.println((getIndexCount() / 3) + " triangles");

        initBuffers(gl2);

//        initVertexArray(gl2);
        float[] modelMin = new float[3];
        float[] modelMax = new float[3];
        computeBoundingBox(modelMin, modelMax);

        float[] diag = new float[]{modelMax[0] - modelMin[0],
            modelMax[1] - modelMin[1],
            modelMax[2] - modelMin[2]};
        scale = (float) (1.0 / Math.sqrt(diag[0] * diag[0] + diag[1] * diag[1] + diag[2] * diag[2]) * 1.5);
        trans = new float[]{(float) (-scale * (modelMin[0] + 0.5 * diag[0])), (float) (-scale * (modelMin[1] + 0.5 * diag[1])),
            (float) (-scale * (modelMin[2] + 0.5 * diag[2]))};
        Viewer.scale = scale;
        Viewer.trans = trans;
    }

    private void initBuffers(GL2 gl2) {

        int totalVertexSize = getCompiledVertexCount() * Float.BYTES;
        int totalIndexSize = getCompiledIndexCount() * Integer.BYTES;

        gl2.glGenBuffers(Buffer.MAX, bufferName);

        gl2.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl2.glBufferData(GL_ARRAY_BUFFER, totalVertexSize, getCompiledVertices(), GL_STATIC_DRAW);
        gl2.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl2.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl2.glBufferData(GL_ELEMENT_ARRAY_BUFFER, totalIndexSize, getCompiledIndices(), GL_STATIC_DRAW);
        gl2.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void initVertexArray(GL2 gl2) {

        int stride = getCompiledVertexSize() * Float.BYTES;
        int normalOffset = getCompiledNormalOffset() * Float.BYTES;

        gl2.glGenVertexArrays(1, vertexArrayName);

        gl2.glBindVertexArray(vertexArrayName.get(0));

        gl2.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));

        gl2.glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, stride, 0);
        gl2.glVertexAttribPointer(Semantic.Attr.NORMAL, 3, GL_FLOAT, false, stride, normalOffset);

        gl2.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl2.glEnableVertexAttribArray(Semantic.Attr.NORMAL);

        gl2.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
    }

    public void render(GL2 gl2) {

        gl2.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl2.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        int stride = getCompiledVertexSize() * Float.BYTES;
        int normalOffset = getCompiledNormalOffset() * Float.BYTES;
        gl2.glVertexPointer(getPositionSize(), GL_FLOAT, stride, 0);
        gl2.glNormalPointer(GL_FLOAT, stride, normalOffset);
        gl2.glEnableClientState(GL_VERTEX_ARRAY);
        gl2.glEnableClientState(GL_NORMAL_ARRAY);
//
//        gl2.glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, stride, 0);
//        gl2.glVertexAttribPointer(Semantic.Attr.NORMAL, 3, GL_FLOAT, false, stride, normalOffset);
//
//        gl2.glEnableVertexAttribArray(Semantic.Attr.POSITION);
//        gl2.glEnableVertexAttribArray(Semantic.Attr.NORMAL);
//        
//        gl2.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
//        
//        gl.glBindVertexArray(vertexArrayName.get(0));

        gl2.glDrawElements(GL_TRIANGLES, getCompiledIndexCount(), GL_UNSIGNED_INT, 0);
        
//        gl2.glDisableVertexAttribArray(Semantic.Attr.POSITION);
//        gl2.glDisableVertexAttribArray(Semantic.Attr.NORMAL);
//        
//        gl2.glBindBuffer(GL_ARRAY_BUFFER, 0);
//        gl2.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

//        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
//        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
//        gl.glDisableClientState(GL_VERTEX_ARRAY);
//        gl.glDisableClientState(GL_NORMAL_ARRAY);
//
//        gl.glBindVertexArray(0);
        Viewer.g_numGeoPasses++;
    }

    private class IdxSet {

        int pIndex = 0;
        int nIndex = 0;

        boolean lessThan(IdxSet rhs) {
            if (pIndex < rhs.pIndex) {
                return true;
            } else if (pIndex == rhs.pIndex) {
                if (nIndex < rhs.nIndex) {
                    return true;
                }
            }
            return false;
        }
    };

    //
    // loadModelFromFile
    //
    //    This function attempts to determine the type of
    //  the filename passed as a parameter. If it understands
    //  that file type, it attempts to parse and load the file
    //  into its raw data structures. If the file type is
    //  recognized and successfully parsed, the function returns
    //  true, otherwise it returns false.
    //
    //////////////////////////////////////////////////////////////
    private boolean loadModelFromFile(String file) {
        URL fileURL = getClass().getClassLoader().getResource(File.separator + file);
        if (fileURL != null) {
            BufferedReader input = null;
            try {

                input = new BufferedReader(new InputStreamReader(fileURL.openStream()));
                String line = null;
                float[] val = new float[4];
                int[][] idx = new int[3][3];
                boolean hasNormals = false;

                while ((line = input.readLine()) != null) {
                    switch (line.charAt(0)) {
                        case '#':
                            break;

                        case 'v':
                            switch (line.charAt(1)) {

                                case ' ':
                                    line = line.substring(line.indexOf(" ") + 1);
                                    //vertex, 3 or 4 components
                                    val[0] = Float.valueOf(line.substring(0, line.indexOf(" ")));
                                    line = line.substring(line.indexOf(" ") + 1);
                                    val[1] = Float.valueOf(line.substring(0, line.indexOf(" ")));
                                    line = line.substring(line.indexOf(" ") + 1);
                                    val[2] = Float.valueOf(line);
                                    positions.add(val[0]);
                                    positions.add(val[1]);
                                    positions.add(val[2]);
                                    break;

                                case 'n':
                                    //normal, 3 components
                                    line = line.substring(line.indexOf(" ") + 1);
                                    val[0] = Float.valueOf(line.substring(0, line.indexOf(" ")));
                                    line = line.substring(line.indexOf(" ") + 1);
                                    val[1] = Float.valueOf(line.substring(0, line.indexOf(" ")));
                                    line = line.substring(line.indexOf(" ") + 1);
                                    val[2] = Float.valueOf(line);
                                    normals.add(val[0]);
                                    normals.add(val[1]);
                                    normals.add(val[2]);
                                    break;
                            }
                            break;

                        case 'f':
                            //face
                            line = line.substring(line.indexOf(" ") + 2);

                            idx[0][0] = Integer.valueOf(line.substring(0, line.indexOf("//"))).intValue();
                            line = line.substring(line.indexOf("//") + 2);
                            idx[0][1] = Integer.valueOf(line.substring(0, line.indexOf(" "))).intValue();

                             {
                                //This face has vertex and normal indices

                                // in .obj, f v1 .... the vertex index used start from 1, so -1 here
                                //remap them to the right spot
                                idx[0][0] = (idx[0][0] > 0) ? (idx[0][0] - 1) : ((int) positions.size() - idx[0][0]);
                                idx[0][1] = (idx[0][1] > 0) ? (idx[0][1] - 1) : ((int) normals.size() - idx[0][1]);

                                //grab the second vertex to prime
                                line = line.substring(line.indexOf(" ") + 1);
                                idx[1][0] = Integer.valueOf(line.substring(0, line.indexOf("//")));
                                line = line.substring(line.indexOf("//") + 2);
                                idx[1][1] = Integer.valueOf(line.substring(0, line.indexOf(" ")));

                                //remap them to the right spot
                                idx[1][0] = (idx[1][0] > 0) ? (idx[1][0] - 1) : ((int) positions.size() - idx[1][0]);
                                idx[1][1] = (idx[1][1] > 0) ? (idx[1][1] - 1) : ((int) normals.size() - idx[1][1]);

                                //grab the third vertex to prime
                                line = line.substring(line.indexOf(" ") + 1);
                                idx[2][0] = Integer.valueOf(line.substring(0, line.indexOf("//")));
                                line = line.substring(line.indexOf("//") + 2);
                                idx[2][1] = Integer.valueOf(line);
                                {
                                    //remap them to the right spot
                                    idx[2][0] = (idx[2][0] > 0) ? (idx[2][0] - 1) : ((int) positions.size() - idx[2][0]);
                                    idx[2][1] = (idx[2][1] > 0) ? (idx[2][1] - 1) : ((int) normals.size() - idx[2][1]);

                                    //add the indices
                                    for (int ii = 0; ii < 3; ii++) {
                                        pIndex.add(idx[ii][0]);
                                        nIndex.add(idx[ii][1]);
                                    }

                                    //prepare for the next iteration, the num 0 does not change.
                                    idx[1][0] = idx[2][0];
                                    idx[1][1] = idx[2][1];
                                }
                                hasNormals = true;
                            }
                            break;

                        default:
                            break;
                    };
                }
                //post-process data
                //free anything that ended up being unused
                if (!hasNormals) {
                    normals.clear();
                    nIndex.clear();
                }

                posSize = 3;
                return true;

            } catch (FileNotFoundException kFNF) {
                System.err.println("Unable to find the shader file " + file);
            } catch (IOException | NumberFormatException kIO) {
                System.err.println("Problem reading the shader file " + file);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException closee) {
                }
            }
        }
        return false;
    }

    //
    //  compileModel
    //
    //    This function takes the raw model data in the internal
    //  structures, and attempts to bring it to a format directly
    //  accepted for vertex array style rendering. This means that
    //  a unique compiled vertex will exist for each unique
    //  combination of position, normal, tex coords, etc that are
    //  used in the model. The prim parameter, tells the model
    //  what type of index list to compile. By default it compiles
    //  a simple triangle mesh with no connectivity. 
    //
    private void compileModel() {
        boolean needsTriangles = true;

        HashMap<IdxSet, Integer> pts = new HashMap<IdxSet, Integer>();
        vertices = FloatBuffer.allocate((pIndex.size() + nIndex.size()) * 3);
        indices = IntBuffer.allocate(pIndex.size());
        for (int i = 0; i < pIndex.size(); i++) {
            IdxSet idx = new IdxSet();
            idx.pIndex = pIndex.get(i);

            if (normals.size() > 0) {
                idx.nIndex = nIndex.get(i);
            } else {
                idx.nIndex = 0;
            }

            if (!pts.containsKey(idx)) {
                if (needsTriangles) {
                    indices.put(pts.size());
                }

                pts.put(idx, pts.size());

                //position, 
                vertices.put(positions.get(idx.pIndex * posSize));
                vertices.put(positions.get(idx.pIndex * posSize + 1));
                vertices.put(positions.get(idx.pIndex * posSize + 2));

                //normal
                if (normals.size() > 0) {
                    vertices.put(normals.get(idx.nIndex * 3));
                    vertices.put(normals.get(idx.nIndex * 3 + 1));
                    vertices.put(normals.get(idx.nIndex * 3 + 2));
                }

            } else if (needsTriangles) {
                indices.put(pts.get(idx));
            }
        }

        //create selected prim
        //set the offsets and vertex size
        pOffset = 0; //always first
        vtxSize = posSize;
        if (hasNormals()) {
            nOffset = vtxSize;
            vtxSize += 3;
        } else {
            nOffset = -1;
        }
        vertices.rewind();
        indices.rewind();
    }

    //
    //  computeBoundingBox
    //
    //    This function returns the points defining the axis-
    //  aligned bounding box containing the model.
    //
    //////////////////////////////////////////////////////////////
    public void computeBoundingBox(float[] minVal, float[] maxVal) {
        if (positions.isEmpty()) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            minVal[i] = 1e10f;
            maxVal[i] = -1e10f;
        }

        for (int i = 0; i < positions.size(); i += 3) {
            float x = positions.get(i);
            float y = positions.get(i + 1);
            float z = positions.get(i + 2);
            minVal[0] = Math.min(minVal[0], x);
            minVal[1] = Math.min(minVal[1], y);
            minVal[2] = Math.min(minVal[2], z);
            maxVal[0] = Math.max(maxVal[0], x);
            maxVal[1] = Math.max(maxVal[1], y);
            maxVal[2] = Math.max(maxVal[2], z);
        }
    }

    public void clearNormals() {
        normals.clear();
        nIndex.clear();
    }

    public boolean hasNormals() {
        return normals.size() > 0;
    }

    public int getPositionSize() {
        return posSize;
    }

    public int getNormalSize() {
        return 3;
    }

    public ArrayList<Float> getPositions() {
        return (positions.size() > 0) ? positions : null;
    }

    public ArrayList<Float> getNormals() {
        return (normals.size() > 0) ? normals : null;
    }

    public ArrayList<Integer> getPositionIndices() {
        return (pIndex.size() > 0) ? pIndex : null;
    }

    public ArrayList<Integer> getNormalIndices() {
        return (nIndex.size() > 0) ? nIndex : null;
    }

    public int getPositionCount() {
        return (posSize > 0) ? positions.size() / posSize : 0;
    }

    public int getNormalCount() {
        return normals.size() / 3;
    }

    public int getIndexCount() {
        return pIndex.size();
    }

    public FloatBuffer getCompiledVertices() {
        return vertices;
    }

    public IntBuffer getCompiledIndices() {
        return indices;
    }

    public int getCompiledPositionOffset() {
        return pOffset;
    }

    public int getCompiledNormalOffset() {
        return nOffset;
    }

    public int getCompiledVertexSize() {
        return vtxSize;
    }

    public int getCompiledVertexCount() {
        return ((pIndex.size() + nIndex.size()) * 3);
    }

    public int getCompiledIndexCount() {
        return pIndex.size();
    }
};
