/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3;

import com.jogamp.common.nio.Buffers;
import static com.jogamp.opengl.GL.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import glm.vec._3.Vec3;
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
 * @author gbarbieri
 */
public final class Model {

    private ArrayList<Float> positions;
    private ArrayList<Float> normals;
    private int posSize;
    private ArrayList<Integer> pIndex;
    private ArrayList<Integer> nIndex;
    private IntBuffer indices;
    private FloatBuffer vertices;
    private int pOffset;
    private int nOffset;
    private int vtxSize;

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int MAX = 2;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1);
    private int[] objects;
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

        load(Viewer.MODEL);

        compile();

        computeBoundingBox();

        System.out.println(getPositionCount() + " vertices");
        System.out.println(getIndexCount() / 3 + " triangles");

        objects = new int[Objects.size.ordinal()];

        initBuffer(gl3);

        initVertexArray(gl3);
    }

    private void initBuffer(GL3 gl3) {

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl3.glBufferData(GL_ARRAY_BUFFER, getCompiledVertexCount() * Float.BYTES, vertices, GL_STATIC_DRAW);

        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl3.glBufferData(GL_ELEMENT_ARRAY_BUFFER, getCompiledIndexCount() * Integer.BYTES, indices, GL_STATIC_DRAW);
    }

    private void initVertexArray(GL3 gl3) {

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));

        gl3.glGenVertexArrays(1, objects, Objects.vao.ordinal());
        gl3.glBindVertexArray(objects[Objects.vao.ordinal()]);
        {
            gl3.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
            {
                gl3.glEnableVertexAttribArray(0);
                gl3.glEnableVertexAttribArray(1);
                {
                    int stride = vtxSize * Buffers.SIZEOF_FLOAT;
                    int offset = pOffset;
                    gl3.glVertexAttribPointer(0, 3, GL3.GL_FLOAT, false, stride, offset);
                    offset = nOffset * Buffers.SIZEOF_FLOAT;
                    gl3.glVertexAttribPointer(1, 3, GL3.GL_FLOAT, false, stride, offset);
                }
            }
        }
        gl3.glBindVertexArray(0);

//        gl3.glGenVertexArrays(1, vertexArrayName);
//        gl3.glBindVertexArray(vertexArrayName.get(0));
//
//        gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, Vec3.SIZE * 2, 0);
//        gl3.glVertexAttribPointer(Semantic.Attr.NORMAL, 3, GL_FLOAT, false, Vec3.SIZE * 2, Vec3.SIZE);
//
//        gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
//        gl3.glEnableVertexAttribArray(Semantic.Attr.NORMAL);
//
//        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
    }

    public void render(GL3 gl3) {

        gl3.glBindVertexArray(objects[Objects.vao.ordinal()]);
        gl3.glDrawElements(GL3.GL_TRIANGLES, getCompiledIndexCount(), GL3.GL_UNSIGNED_INT, 0);
    }

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

    public void computeBoundingBox() {

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

    private enum Objects {

        vbo,
        ibo,
        vao,
        size
    }
}
