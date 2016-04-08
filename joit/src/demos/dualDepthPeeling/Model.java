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
import com.jogamp.opengl.GL2;
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
import java.util.Vector;

public final class Model {

    private ArrayList<Float> positions_ = new ArrayList<>();
    private ArrayList<Float> normals_ = new ArrayList<>();
    private int posSize_;

    Vector<Integer> pIndex_ = new Vector<Integer>();
    Vector<Integer> nIndex_ = new Vector<Integer>();

    //data structures optimized for rendering, compiled model
    IntBuffer indices_ = null;
    FloatBuffer vertices_ = null;
    int pOffset_;
    int nOffset_;
    int vtxSize_ = 0;

    int openEdges_;

    private int[] g_vboId = new int[1];
    private int[] g_eboId = new int[1];
    public static float scale;
    public static float[] trans;

    public Model(GL2 gl2, String filename) {

        posSize_ = 0;
        pOffset_ = -1;
        nOffset_ = -1;
        vtxSize_ = 0;
        openEdges_ = 0;

        System.err.println("loading OBJ...\n");

        loadModelFromFile(filename);

        System.err.println("compiling mesh...\n");
        compileModel();

        System.err.println(getPositionCount() + " vertices");
        System.err.println((getIndexCount() / 3) + " triangles");
        int totalVertexSize = getCompiledVertexCount() * Float.BYTES;
        int totalIndexSize = getCompiledIndexCount() * Integer.BYTES;

        gl2.glGenBuffers(1, g_vboId, 0);
        gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, g_vboId[0]);
        gl2.glBufferData(GL2.GL_ARRAY_BUFFER, totalVertexSize, getCompiledVertices(), GL2.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

        gl2.glGenBuffers(1, g_eboId, 0);
        gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, g_eboId[0]);
        gl2.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, totalIndexSize, getCompiledIndices(), GL2.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);

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

    public void render(GL2 gl) {

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, g_vboId[0]);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, g_eboId[0]);
        int stride = getCompiledVertexSize() * Float.BYTES;
        int normalOffset = getCompiledNormalOffset() * Float.BYTES;
        gl.glVertexPointer(getPositionSize(), GL2.GL_FLOAT, stride, 0);
        gl.glNormalPointer(GL2.GL_FLOAT, stride, normalOffset);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);

        gl.glDrawElements(GL2.GL_TRIANGLES, getCompiledIndexCount(), GL2.GL_UNSIGNED_INT, 0);

        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);

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
                                    positions_.add(val[0]);
                                    positions_.add(val[1]);
                                    positions_.add(val[2]);
                                    break;

                                case 'n':
                                    //normal, 3 components
                                    line = line.substring(line.indexOf(" ") + 1);
                                    val[0] = Float.valueOf(line.substring(0, line.indexOf(" ")));
                                    line = line.substring(line.indexOf(" ") + 1);
                                    val[1] = Float.valueOf(line.substring(0, line.indexOf(" ")));
                                    line = line.substring(line.indexOf(" ") + 1);
                                    val[2] = Float.valueOf(line);
                                    normals_.add(val[0]);
                                    normals_.add(val[1]);
                                    normals_.add(val[2]);
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
                                idx[0][0] = (idx[0][0] > 0) ? (idx[0][0] - 1) : ((int) positions_.size() - idx[0][0]);
                                idx[0][1] = (idx[0][1] > 0) ? (idx[0][1] - 1) : ((int) normals_.size() - idx[0][1]);

                                //grab the second vertex to prime
                                line = line.substring(line.indexOf(" ") + 1);
                                idx[1][0] = Integer.valueOf(line.substring(0, line.indexOf("//")));
                                line = line.substring(line.indexOf("//") + 2);
                                idx[1][1] = Integer.valueOf(line.substring(0, line.indexOf(" ")));

                                //remap them to the right spot
                                idx[1][0] = (idx[1][0] > 0) ? (idx[1][0] - 1) : ((int) positions_.size() - idx[1][0]);
                                idx[1][1] = (idx[1][1] > 0) ? (idx[1][1] - 1) : ((int) normals_.size() - idx[1][1]);

                                //grab the third vertex to prime
                                line = line.substring(line.indexOf(" ") + 1);
                                idx[2][0] = Integer.valueOf(line.substring(0, line.indexOf("//")));
                                line = line.substring(line.indexOf("//") + 2);
                                idx[2][1] = Integer.valueOf(line);
                                {
                                    //remap them to the right spot
                                    idx[2][0] = (idx[2][0] > 0) ? (idx[2][0] - 1) : ((int) positions_.size() - idx[2][0]);
                                    idx[2][1] = (idx[2][1] > 0) ? (idx[2][1] - 1) : ((int) normals_.size() - idx[2][1]);

                                    //add the indices
                                    for (int ii = 0; ii < 3; ii++) {
                                        pIndex_.add(idx[ii][0]);
                                        nIndex_.add(idx[ii][1]);
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
                    normals_.clear();
                    nIndex_.clear();
                }

                posSize_ = 3;
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
        vertices_ = FloatBuffer.allocate((pIndex_.size() + nIndex_.size()) * 3);
        indices_ = IntBuffer.allocate(pIndex_.size());
        for (int i = 0; i < pIndex_.size(); i++) {
            IdxSet idx = new IdxSet();
            idx.pIndex = pIndex_.elementAt(i);

            if (normals_.size() > 0) {
                idx.nIndex = nIndex_.elementAt(i);
            } else {
                idx.nIndex = 0;
            }

            if (!pts.containsKey(idx)) {
                if (needsTriangles) {
                    indices_.put(pts.size());
                }

                pts.put(idx, pts.size());

                //position, 
                vertices_.put(positions_.get(idx.pIndex * posSize_));
                vertices_.put(positions_.get(idx.pIndex * posSize_ + 1));
                vertices_.put(positions_.get(idx.pIndex * posSize_ + 2));

                //normal
                if (normals_.size() > 0) {
                    vertices_.put(normals_.get(idx.nIndex * 3));
                    vertices_.put(normals_.get(idx.nIndex * 3 + 1));
                    vertices_.put(normals_.get(idx.nIndex * 3 + 2));
                }

            } else if (needsTriangles) {
                indices_.put(pts.get(idx));
            }
        }

        //create selected prim
        //set the offsets and vertex size
        pOffset_ = 0; //always first
        vtxSize_ = posSize_;
        if (hasNormals()) {
            nOffset_ = vtxSize_;
            vtxSize_ += 3;
        } else {
            nOffset_ = -1;
        }
        vertices_.rewind();
        indices_.rewind();
    }

    //
    //  computeBoundingBox
    //
    //    This function returns the points defining the axis-
    //  aligned bounding box containing the model.
    //
    //////////////////////////////////////////////////////////////
    public void computeBoundingBox(float[] minVal, float[] maxVal) {
        if (positions_.isEmpty()) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            minVal[i] = 1e10f;
            maxVal[i] = -1e10f;
        }

        for (int i = 0; i < positions_.size(); i += 3) {
            float x = positions_.get(i);
            float y = positions_.get(i + 1);
            float z = positions_.get(i + 2);
            minVal[0] = Math.min(minVal[0], x);
            minVal[1] = Math.min(minVal[1], y);
            minVal[2] = Math.min(minVal[2], z);
            maxVal[0] = Math.max(maxVal[0], x);
            maxVal[1] = Math.max(maxVal[1], y);
            maxVal[2] = Math.max(maxVal[2], z);
        }
    }

    public void clearNormals() {
        normals_.clear();
        nIndex_.clear();
    }

    public boolean hasNormals() {
        return normals_.size() > 0;
    }

    public int getPositionSize() {
        return posSize_;
    }

    public int getNormalSize() {
        return 3;
    }

    public ArrayList<Float> getPositions() {
        return (positions_.size() > 0) ? positions_ : null;
    }

    public ArrayList<Float> getNormals() {
        return (normals_.size() > 0) ? normals_ : null;
    }

    public Vector<Integer> getPositionIndices() {
        return (pIndex_.size() > 0) ? pIndex_ : null;
    }

    public Vector<Integer> getNormalIndices() {
        return (nIndex_.size() > 0) ? nIndex_ : null;
    }

    public int getPositionCount() {
        return (posSize_ > 0) ? positions_.size() / posSize_ : 0;
    }

    public int getNormalCount() {
        return normals_.size() / 3;
    }

    public int getIndexCount() {
        return pIndex_.size();
    }

    public FloatBuffer getCompiledVertices() {
        return vertices_;
    }

    public IntBuffer getCompiledIndices() {
        return indices_;
    }

    public int getCompiledPositionOffset() {
        return pOffset_;
    }

    public int getCompiledNormalOffset() {
        return nOffset_;
    }

    public int getCompiledVertexSize() {
        return vtxSize_;
    }

    public int getCompiledVertexCount() {
        return ((pIndex_.size() + nIndex_.size()) * 3);
    }

    public int getCompiledIndexCount() {
        return pIndex_.size();
    }

    public int getOpenEdgeCount() {
        return openEdges_;
    }
};
