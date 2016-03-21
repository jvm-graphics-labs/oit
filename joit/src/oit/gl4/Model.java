/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl4;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import com.jogamp.opengl.GL4;
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
    private IntBuffer elements;
    private FloatBuffer vertices;
    private int pOffset_;
    private int nOffset;
    private int vtxSize;
    private int[] objects;
    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1);
    /**
     * https://jogamp.org/bugzilla/show_bug.cgi?id=1287
     */
    private boolean bug1287 = true;

    public Model(GL4 gl4, String filename) throws IOException {

        positions = new ArrayList<>();
        normals = new ArrayList<>();

        pIndex = new ArrayList<>();
        nIndex = new ArrayList<>();

        elements = null;
        vertices = null;
        vtxSize = 0;

        load(filename);

        compile();

        System.out.println(getPositionCount() + " vertices");
        System.out.println(getIndexCount() / 3 + " triangles");
        
        initBuffers(gl4);
        initVertexArray(gl4);
    }

    public void render(GL4 gl4) {

        gl4.glBindVertexArray(vertexArrayName.get(0));
        
        gl4.glDrawElements(GL4.GL_TRIANGLES, getCompiledIndexCount(), GL4.GL_UNSIGNED_INT, 0);
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
                String line = input.readLine();
                float[] val = new float[4];
                int[][] idx = new int[3][3];
                boolean hasNormals = false;

                while (line != null) {

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
                    line = input.readLine();
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
        elements = IntBuffer.allocate(pIndex.size());
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
                    elements.put(pts.size());
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
                elements.put(pts.get(idx));
            }
        }

        //create selected prim
        //set the offsets and vertex size
        pOffset_ = 0; //always first
        vtxSize = posSize;
        if (hasNormals()) {
            nOffset = vtxSize;
            vtxSize += 3;
        } else {
            nOffset = -1;
        }
        vertices.rewind();
        elements.rewind();
    }

    private void initBuffers(GL4 gl4) {

        gl4.glCreateBuffers(Buffer.MAX, bufferName);

        if (!bug1287) {

            gl4.glNamedBufferStorage(bufferName.get(Buffer.VERTEX), vertices.capacity() * Float.BYTES, vertices,
                    GL_STATIC_DRAW);

            gl4.glNamedBufferStorage(bufferName.get(Buffer.ELEMENT), elements.capacity() * Integer.BYTES,
                    elements, GL_STATIC_DRAW);

        } else {

            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl4.glBufferStorage(GL_ARRAY_BUFFER, vertices.capacity() * Float.BYTES, vertices, 0);

            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
            gl4.glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, elements.capacity() * Integer.BYTES, elements, 0);
        }
    }

    private void initVertexArray(GL4 gl4) {

        gl4.glCreateVertexArrays(1, vertexArrayName);

        gl4.glVertexArrayAttribBinding(vertexArrayName.get(0), Semantic.Attr.POSITION, Semantic.Stream._0);
        gl4.glVertexArrayAttribBinding(vertexArrayName.get(0), Semantic.Attr.NORMAL, Semantic.Stream._0);

        gl4.glVertexArrayAttribFormat(vertexArrayName.get(0), Semantic.Attr.POSITION, 3, GL_FLOAT, false, 0);
        gl4.glVertexArrayAttribFormat(vertexArrayName.get(0), Semantic.Attr.NORMAL, 3, GL_FLOAT, false, Vec3.SIZE);

        gl4.glEnableVertexArrayAttrib(vertexArrayName.get(0), Semantic.Attr.POSITION);
        gl4.glEnableVertexArrayAttrib(vertexArrayName.get(0), Semantic.Attr.NORMAL);

        gl4.glVertexArrayElementBuffer(vertexArrayName.get(0), bufferName.get(Buffer.ELEMENT));
        gl4.glVertexArrayVertexBuffer(vertexArrayName.get(0), Semantic.Stream._0, bufferName.get(Buffer.VERTEX), 0,
                2 * Vec3.SIZE);
    }
    
    public class IdxSet {

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

    private int getPositionCount() {
        return (posSize > 0) ? positions.size() / posSize : 0;
    }

    public boolean hasNormals() {
        return normals.size() > 0;
    }

    public final int getIndexCount() {
        return pIndex.size();
    }

    public final int getCompiledIndexCount() {
        return pIndex.size();
    }

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int MAX = 2;
    }
}
