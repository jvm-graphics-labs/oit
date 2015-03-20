/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.dpGl3Official;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.GLBuffers;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import javax.media.opengl.GL3;
import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public final class Model {

    private ArrayList<Float> positions_;
    private ArrayList<Float> normals_;
    private int posSize_;
    private ArrayList<Integer> pIndex_;
    private ArrayList<Integer> nIndex_;
    private IntBuffer indices_;
    private FloatBuffer vertices_;
    private int pOffset_;
    private int nOffset_;
    private int vtxSize_;
    private int[] objects;
    private Vec3 center;

    public Model(GL3 gl3, String filename) throws IOException {

        positions_ = new ArrayList<>();
        normals_ = new ArrayList<>();

        pIndex_ = new ArrayList<>();
        nIndex_ = new ArrayList<>();

        indices_ = null;
        vertices_ = null;
        vtxSize_ = 0;

        load(filename);

        compile();

        computeBoundingBox();

        System.out.println(getPositionCount() + " vertices");
        System.out.println(getIndexCount() / 3 + " triangles");

        objects = new int[Objects.size.ordinal()];

        initVbo(gl3);
        initIbo(gl3);
        initVao(gl3);
    }

    public void render(GL3 gl3) {

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, objects[Objects.vbo.ordinal()]);
        {
            gl3.glBindVertexArray(objects[Objects.vao.ordinal()]);
            {
                //  Render, passing the vertex number
//                gl3.glDrawArrays(GL3.GL_TRIANGLES, 0, vertexAttributes.length / 6);
                gl3.glDrawElements(GL3.GL_TRIANGLES, getCompiledIndexCount(), GL3.GL_UNSIGNED_INT, 0);
            }
            gl3.glBindVertexArray(0);
        }
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

        DepthPeeling.numGeoPasses++;
    }

    public void computeBoundingBox() {

        float[] minVal = new float[3];
        float[] maxVal = new float[3];

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
        System.out.println("min (" + minVal[0] + ", " + minVal[1] + ", " + minVal[2] + ")");
        System.out.println("max (" + maxVal[0] + ", " + maxVal[1] + ", " + maxVal[2] + ")");

        center = new Vec3((maxVal[0] + minVal[0]) / 2, (maxVal[1] + minVal[1]) / 2, (maxVal[2] + minVal[2]) / 2);
        center.print("center");
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

                            idx[0][0] = Integer.valueOf(line.substring(0, line.indexOf("//")));
                            line = line.substring(line.indexOf("//") + 2);
                            idx[0][1] = Integer.valueOf(line.substring(0, line.indexOf(" ")));

                             {
                                //This face has vertex and normal indices

                                // in .obj, f v1 .... the vertex index used start from 1, so -1 here
                                //remap them to the right spot
                                idx[0][0] = (idx[0][0] > 0) ? (idx[0][0] - 1) : (positions_.size() - idx[0][0]);
                                idx[0][1] = (idx[0][1] > 0) ? (idx[0][1] - 1) : (normals_.size() - idx[0][1]);

                                //grab the second vertex to prime
                                line = line.substring(line.indexOf(" ") + 1);
                                idx[1][0] = Integer.valueOf(line.substring(0, line.indexOf("//")));
                                line = line.substring(line.indexOf("//") + 2);
                                idx[1][1] = Integer.valueOf(line.substring(0, line.indexOf(" ")));

                                //remap them to the right spot
                                idx[1][0] = (idx[1][0] > 0) ? (idx[1][0] - 1) : (positions_.size() - idx[1][0]);
                                idx[1][1] = (idx[1][1] > 0) ? (idx[1][1] - 1) : (normals_.size() - idx[1][1]);

                                //grab the third vertex to prime
                                line = line.substring(line.indexOf(" ") + 1);
                                idx[2][0] = Integer.valueOf(line.substring(0, line.indexOf("//")));
                                line = line.substring(line.indexOf("//") + 2);
                                idx[2][1] = Integer.valueOf(line);
                                {
                                    //remap them to the right spot
                                    idx[2][0] = (idx[2][0] > 0) ? (idx[2][0] - 1) : (positions_.size() - idx[2][0]);
                                    idx[2][1] = (idx[2][1] > 0) ? (idx[2][1] - 1) : (normals_.size() - idx[2][1]);

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
                    }
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
        vertices_ = FloatBuffer.allocate((pIndex_.size() + nIndex_.size()) * 3);
        indices_ = IntBuffer.allocate(pIndex_.size());
        for (int i = 0; i < pIndex_.size(); i++) {
            IdxSet idx = new IdxSet();
            idx.pIndex = pIndex_.get(i);

            if (normals_.size() > 0) {
                idx.nIndex = nIndex_.get(i);
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

            } else {
                if (needsTriangles) {
                    indices_.put(pts.get(idx));
                }
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

    private void initVbo(GL3 gl3) {

        gl3.glGenBuffers(1, objects, Objects.vbo.ordinal());

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, objects[Objects.vbo.ordinal()]);
        {
            int totalVertexSize = getCompiledVertexCount() * Buffers.SIZEOF_FLOAT;

            gl3.glBufferData(GL3.GL_ARRAY_BUFFER, totalVertexSize, vertices_, GL3.GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
    }

    private void initIbo(GL3 gl3) {

        gl3.glGenBuffers(1, objects, Objects.ibo.ordinal());

        gl3.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, objects[Objects.ibo.ordinal()]);
        {
            int totalIndexSize = getCompiledIndexCount() * Buffers.SIZEOF_INT;

            gl3.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER, totalIndexSize, indices_, GL3.GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void initVao(GL3 gl3) {

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, objects[Objects.vbo.ordinal()]);

        gl3.glGenVertexArrays(1, objects, Objects.vao.ordinal());
        gl3.glBindVertexArray(objects[Objects.vao.ordinal()]);
        {
            gl3.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, objects[Objects.ibo.ordinal()]);
            {
                gl3.glEnableVertexAttribArray(0);
                gl3.glEnableVertexAttribArray(1);
                {
                    int stride = vtxSize_ * Buffers.SIZEOF_FLOAT;
                    int offset = pOffset_;
                    gl3.glVertexAttribPointer(0, 3, GL3.GL_FLOAT, false, stride, offset);
                    offset = nOffset_ * Buffers.SIZEOF_FLOAT;
                    gl3.glVertexAttribPointer(1, 3, GL3.GL_FLOAT, false, stride, offset);
                }
            }
        }
        gl3.glBindVertexArray(0);
    }

    public Vec3 getCenter() {
        return center;
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

    public final int getPositionCount() {
        return (posSize_ > 0) ? (int) positions_.size() / posSize_ : 0;
    }

    public boolean hasNormals() {
        return normals_.size() > 0;
    }

    public final int getIndexCount() {
        return pIndex_.size();
    }

    public final int getCompiledVertexCount() {
        return ((pIndex_.size() + nIndex_.size()) * 3);
    }

    public final int getCompiledIndexCount() {
        return pIndex_.size();
    }

    private enum Objects {

        vbo,
        ibo,
        vao,
        size
    }
}
