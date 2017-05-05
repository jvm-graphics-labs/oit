/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.framework;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author GBarbieri
 */
public abstract class Model {

    protected ArrayList<Float> positions;
    protected ArrayList<Float> normals;
    protected int posSize;
    protected ArrayList<Integer> pIndex;
    protected ArrayList<Integer> nIndex;
    protected IntBuffer indices;
    protected FloatBuffer vertices;
    protected int pOffset;
    protected int nOffset;
    protected int vtxSize;

    protected class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int MAX = 2;
    }

    protected IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1);
    public static float scale;
    public static float[] trans;

    public Model(GL3 gl3) throws IOException {

        positions = new ArrayList<>();
        normals = new ArrayList<>();

        pIndex = new ArrayList<>();
        nIndex = new ArrayList<>();

        indices = null;
        vertices = null;
        vtxSize = 0;

        load(Resources.MODEL_PATH);

        compile();

        computeBoundingBox();

        System.out.println(getPositionCount() + " vertices");
        System.out.println(getIndexCount() / 3 + " triangles");

        initBuffers(gl3);

        initVertexArray(gl3);
    }

    protected abstract void initBuffers(GL3 gl3);

    protected abstract void initVertexArray(GL3 gl3);

    public abstract void render(GL3 gl3);

    /**
     * loadModelFromFile.
     *
     * This function attempts to determine the type of the filename passed as a
     * parameter. If it understands that file type, it attempts to parse and
     * load the file into its raw data structures. If the file type is
     * recognized and successfully parsed, the function returns true, otherwise
     * it returns false.
     *
     * @param filePath
     * @return
     */
    private boolean load(String filePath) {
        System.out.println("loading OBJ...");
        URL fileUrl = getClass().getResource(filePath);

        if (fileUrl != null) {

            BufferedReader input = null;

            try {

                input = new BufferedReader(new InputStreamReader(fileUrl.openStream()));
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

                            idx[0][0] = Integer.valueOf(line.substring(0, line.indexOf("//")));
                            line = line.substring(line.indexOf("//") + 2);
                            idx[0][1] = Integer.valueOf(line.substring(0, line.indexOf(" ")));

                             {
                                //This face has vertex and normal indices

                                // in .obj, f v1 .... the vertex index used start from 1, so -1 here
                                //remap them to the right spot
                                idx[0][0] = (idx[0][0] > 0) ? (idx[0][0] - 1) : (positions.size() - idx[0][0]);
                                idx[0][1] = (idx[0][1] > 0) ? (idx[0][1] - 1) : (normals.size() - idx[0][1]);

                                //grab the second vertex to prime
                                line = line.substring(line.indexOf(" ") + 1);
                                idx[1][0] = Integer.valueOf(line.substring(0, line.indexOf("//")));
                                line = line.substring(line.indexOf("//") + 2);
                                idx[1][1] = Integer.valueOf(line.substring(0, line.indexOf(" ")));

                                //remap them to the right spot
                                idx[1][0] = (idx[1][0] > 0) ? (idx[1][0] - 1) : (positions.size() - idx[1][0]);
                                idx[1][1] = (idx[1][1] > 0) ? (idx[1][1] - 1) : (normals.size() - idx[1][1]);

                                //grab the third vertex to prime
                                line = line.substring(line.indexOf(" ") + 1);
                                idx[2][0] = Integer.valueOf(line.substring(0, line.indexOf("//")));
                                line = line.substring(line.indexOf("//") + 2);
                                idx[2][1] = Integer.valueOf(line);
                                {
                                    //remap them to the right spot
                                    idx[2][0] = (idx[2][0] > 0) ? (idx[2][0] - 1) : (positions.size() - idx[2][0]);
                                    idx[2][1] = (idx[2][1] > 0) ? (idx[2][1] - 1) : (normals.size() - idx[2][1]);

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
                    }
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
                System.err.println("Unable to find the shader file " + filePath);
            } catch (IOException | NumberFormatException kIO) {
                System.err.println("Problem reading the shader file " + filePath);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException close) {
                }
            }
        }
        return false;
    }

    /**
     * compileModel.
     *
     * This function takes the raw model data in the internal structures, and
     * attempts to bring it to a format directly accepted for vertex array style
     * rendering. This means that a unique compiled vertex will exist for each
     * unique combination of position, normal, tex coords, etc that are used in
     * the model. The prim parameter, tells the model what type of index list to
     * compile. By default it compiles a simple triangle mesh with no
     * connectivity.
     */
    private void compile() {
        System.out.println("compiling mesh...");
        boolean needsTriangles = true;

        HashMap<IdxSet, Integer> pts = new HashMap<>();
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

    private void computeBoundingBox() {

        float[] modelMin = new float[3];
        float[] modelMax = new float[3];

        if (positions.isEmpty()) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            modelMin[i] = 1e10f;
            modelMax[i] = -1e10f;
        }

        for (int i = 0; i < positions.size(); i += 3) {
            float x = positions.get(i);
            float y = positions.get(i + 1);
            float z = positions.get(i + 2);
            modelMin[0] = Math.min(modelMin[0], x);
            modelMin[1] = Math.min(modelMin[1], y);
            modelMin[2] = Math.min(modelMin[2], z);
            modelMax[0] = Math.max(modelMax[0], x);
            modelMax[1] = Math.max(modelMax[1], y);
            modelMax[2] = Math.max(modelMax[2], z);
        }

        float[] diag = new float[]{modelMax[0] - modelMin[0], modelMax[1] - modelMin[1],
            modelMax[2] - modelMin[2]};
        scale = (float) (1.0 / Math.sqrt(diag[0] * diag[0] + diag[1] * diag[1] + diag[2] * diag[2]) * 1.5);
        trans = new float[]{(float) (-scale * (modelMin[0] + 0.5 * diag[0])), (float) (-scale * (modelMin[1] + 0.5 * diag[1])),
            (float) (-scale * (modelMin[2] + 0.5 * diag[2]))};
    }

    private class IdxSet {

        Integer pIndex = 0;
        Integer nIndex = 0;

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

    public final int getPositionCount() {
        return (posSize > 0) ? positions.size() / posSize : 0;
    }

    public boolean hasNormals() {
        return normals.size() > 0;
    }

    public final int getIndexCount() {
        return pIndex.size();
    }

    public final int getCompiledVertexCount() {
        return ((pIndex.size() + nIndex.size()) * 3);
    }

    public final int getCompiledIndexCount() {
        return pIndex.size();
    }
}
