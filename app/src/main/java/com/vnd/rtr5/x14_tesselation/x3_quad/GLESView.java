package com.vnd.rtr5.x14_tesselation.x3_quad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.rtr5.common.VertexAttributesEnum;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLESView extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final int[] vbo_position_triangle = new int[1];
    private final int[] vao_triangle = new int[1];
    private final int[] maxTesselationLevel = new int[1];
    private final float[] perspectiveProjectionMatrix = new float[16];
    private GestureDetector gestureDetector = null;
    // noprivate intin java
    private int shaderProgramObject = 0;
    private int mvpMatrixUniform;
    private int colorUniform = 0;
    // Tesselation related global variables
    private int rectangleXSubdivisionUniform = 0;
    private int rectangleYSubdivisionUniform = 0;
    private int edge1SubdivisionUniform = 0;
    private int edge2SubdivisionUniform = 0;
    private int edge3SubdivisionUniform = 0;
    private int edge4SubdivisionUniform = 0;
    private int rectangleXSubdivisions = 0;
    private int rectangleYSubdivisions = 0;
    private int edge1Subdivisions = 1;
    private int edge2Subdivisions = 1;
    private int edge3Subdivisions = 1;
    private int edge4Subdivisions = 1;

    public GLESView(Context context) {
        super(context);

        // OpenGL ES related
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // when invalidate rect on windows

        // event related
        // create and set gestureDetector object
        gestureDetector = new GestureDetector(context, this, null, false);
        gestureDetector.setOnDoubleTapListener(this);
    }

    // implementation of 3 methods of GLSurfaceView.renderer interface
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initialize(gl);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        resize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        display();
        update();
    }

    // implementation of onTouch event of ViewClass
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!gestureDetector.onTouchEvent(event)) {
            return super.onTouchEvent(event);
        }
        return true;
    }

    // implementation of 3 methods of onDoubleTap listener interface
    @Override
    public boolean onDoubleTap(@NonNull MotionEvent event) {
        rectangleYSubdivisions = (rectangleYSubdivisions + 1) % maxTesselationLevel[0];
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        rectangleXSubdivisions = (rectangleXSubdivisions + 1) % maxTesselationLevel[0];
        return true;
    }

    // implementation of 6 methods of onGesture listener interface
    @Override
    public boolean onDown(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, @NonNull MotionEvent event2, float velocityX, float velocityY) {
        return true;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent event) {
        edge1Subdivisions = (edge1Subdivisions + 1) % maxTesselationLevel[0];
        edge2Subdivisions = (edge2Subdivisions + 1) % maxTesselationLevel[0];
        edge3Subdivisions = (edge3Subdivisions + 1) % maxTesselationLevel[0];
        edge4Subdivisions = (edge4Subdivisions + 1) % maxTesselationLevel[0];
    }

    @Override
    public boolean onScroll(MotionEvent event1, @NonNull MotionEvent event2, float distanceX, float distanceY) {
        return true;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent event) {
    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent event) {
        return true;
    }

    // implementation of private methods
    private void initialize(GL10 gl) {
        // code
        printGLInfo(gl);

        // vertex shader
        final String vertexShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "in vec4 aPosition;" +
                        "uniform mat4 uMVPMatrix;" +
                        "void main(void)" +
                        "{" +
                        "gl_Position = aPosition;" +
                        "}";
        int vertexShaderObject = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
        GLES32.glShaderSource(vertexShaderObject, vertexShaderSourceCode);
        GLES32.glCompileShader(vertexShaderObject);
        int[] status = new int[1];
        int[] infoLogLength = new int[1];
        String szInfoLog;
        GLES32.glGetShaderiv(vertexShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(vertexShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(vertexShaderObject);
                System.out.println("VND: vertex shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }
// tesselation control shader
        final String tcsShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "layout (vertices = 4) out;" +
                        "uniform int uRectangleXSubdivision;" +
                        "uniform int uRectangleYSubdivision;" +
                        "uniform int uEdge1Subdivision;" +
                        "uniform int uEdge2Subdivision;" +
                        "uniform int uEdge3Subdivision;" +
                        "uniform int uEdge4Subdivision;" +
                        "void main(void)" +
                        "{" +
                        "if (gl_InvocationID == 0)" +
                        "{" +
                        // The two elements of the gl_TessLevelInner[] array should be written
                        // by the tessellation control shader and control the level of
                        // tessellation applied to the innermost region within the quad. The
                        // first element sets the tessellation applied in the horizontal (u)
                        // direction and the second element sets the tessellation level applied
                        // in the vertical (v) direction.
                        "gl_TessLevelInner[0] = float(uRectangleXSubdivision);" +
                        "gl_TessLevelInner[1] = float(uRectangleYSubdivision);" +
                        // Also, all four elements of the gl_TessLevelOuter[] array should be
                        // written by the tessellation control shader and are used to determine
                        // the level of tessellation applied to the outer edges of the quad.
                        "gl_TessLevelOuter[0] = float(uEdge1Subdivision);" +
                        "gl_TessLevelOuter[1] = float(uEdge2Subdivision);" +
                        "gl_TessLevelOuter[2] = float(uEdge3Subdivision);" +
                        "gl_TessLevelOuter[3] = float(uEdge4Subdivision);" +
                        "}" +
                        "gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;" +
                        "}";
        int tcsShaderObject = GLES32.glCreateShader(GLES32.GL_TESS_CONTROL_SHADER);
        GLES32.glShaderSource(tcsShaderObject, tcsShaderSourceCode);
        GLES32.glCompileShader(tcsShaderObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(tcsShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(tcsShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(tcsShaderObject);
                System.out.println("VND: tcs shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // tesselation evaluation shader
        final String tesShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        // When the chosen tessellation mode is set to quads, the tessellation
                        // engine will generate a quadrilateral (or quad) and break it up into
                        // a set of triangles.
                        "layout (quads, point_mode) in;" +
                        "uniform mat4 uMVPMatrix;" +
                        "void main(void)" +
                        "{" +
                        "vec4 p1 = mix(gl_in[0].gl_Position, gl_in[1].gl_Position, gl_TessCoord.x);" +
                        "vec4 p2 = mix(gl_in[3].gl_Position, gl_in[2].gl_Position, gl_TessCoord.x);" +
                        "gl_PointSize = 10.0;" +
                        "gl_Position = uMVPMatrix * mix(p1, p2, gl_TessCoord.y);" +
                        "}";
        int tesShaderObject = GLES32.glCreateShader(GLES32.GL_TESS_EVALUATION_SHADER);
        GLES32.glShaderSource(tesShaderObject, tesShaderSourceCode);
        GLES32.glCompileShader(tesShaderObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(tesShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(tesShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(tesShaderObject);
                System.out.println("VND: tes shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // fragment shader
        final String fragmentShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "uniform vec4 uColor;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = uColor;" +
                        "}";
        int fragmentShaderObject = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
        GLES32.glShaderSource(fragmentShaderObject, fragmentShaderSourceCode);
        GLES32.glCompileShader(fragmentShaderObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(fragmentShaderObject, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(fragmentShaderObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(fragmentShaderObject);
                System.out.println("VND: fragment shader compilation error log: " + szInfoLog);
            }
            uninitialize();
        }

        // Shader program
        shaderProgramObject = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject, vertexShaderObject);
        GLES32.glAttachShader(shaderProgramObject, tcsShaderObject);
        GLES32.glAttachShader(shaderProgramObject, tesShaderObject);
        GLES32.glAttachShader(shaderProgramObject, fragmentShaderObject);
        GLES32.glBindAttribLocation(shaderProgramObject, VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, "aPosition");
        GLES32.glLinkProgram(shaderProgramObject);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetProgramiv(shaderProgramObject, GLES32.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetProgramiv(shaderProgramObject, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject);
                System.out.println("VND: shader program linking error log: " + szInfoLog);
            }
            uninitialize();
        }

        // get shader uniform locations - must be after linkage
        mvpMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uMVPMatrix");
        colorUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uColor");
        rectangleXSubdivisionUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uRectangleXSubdivision");
        rectangleYSubdivisionUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uRectangleYSubdivision");
        edge1SubdivisionUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uEdge1Subdivision");
        edge2SubdivisionUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uEdge2Subdivision");
        edge3SubdivisionUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uEdge3Subdivision");
        edge4SubdivisionUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uEdge4Subdivision");

        final float[] rectangle_position = {
                1.0f, 1.0f, 0.0f,
                -1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 0.0f,
                1.0f, -1.0f, 0.0f
        };

        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao_triangle, 0);
        GLES32.glBindVertexArray(vao_triangle[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position_triangle, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position_triangle[0]);
        // prepare triangle vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferTrianglePosition = ByteBuffer.allocateDirect(rectangle_position.length * 4);
        byteBufferTrianglePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferTrianglePosition = byteBufferTrianglePosition.asFloatBuffer();
        floatBufferTrianglePosition.put(rectangle_position);
        floatBufferTrianglePosition.position(0);
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, rectangle_position.length * 4, floatBufferTrianglePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

        // Get maximum tesselation level
        GLES32.glGetIntegerv(GLES32.GL_MAX_TESS_GEN_LEVEL, maxTesselationLevel, 0);
        System.out.println("VND: maxTesselationLevel: " + maxTesselationLevel[0]);

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // Disable culling
        GLES32.glDisable(GLES32.GL_CULL_FACE);

        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // initialize perspectiveProjectionMatrix
        Matrix.setIdentityM(perspectiveProjectionMatrix, 0);
    }

    private void printGLInfo(GL10 gl) {
        // code
        System.out.println("VND: OpenGL ES Vendor: " + gl.glGetString(GL10.GL_VENDOR));
        System.out.println("VND: OpenGL ES Renderer: " + gl.glGetString(GL10.GL_RENDERER));
        System.out.println("VND: OpenGL ES Version: " + gl.glGetString(GL10.GL_VERSION));
        System.out.println("VND: OpenGL ES Shading Language Version: " + gl.glGetString(GLES32.GL_SHADING_LANGUAGE_VERSION));

        // listing of supported extensions
        int[] retVal = new int[1];
        GLES32.glGetIntegerv(GLES32.GL_NUM_EXTENSIONS, retVal, 0);
        int numExtensions = retVal[0];
        for (int i = 0; i < numExtensions; i++) {
            System.out.println("VND: " + GLES32.glGetStringi(GL10.GL_EXTENSIONS, i));
        }
    }

    private void resize(int width, int height) {
        // code
        if (height <= 0) {
            height = 1;
        }

        if (width <= 0) {
            width = 1;
        }

        // Viewport == binocular
        GLES32.glViewport(0, 0, width, height);

        // set perspective projection matrix
        Matrix.perspectiveM(perspectiveProjectionMatrix, 0, 45.0f, ((float) width / (float) height), 0.1f, 100.0f);
    }

    private void display() {
        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        GLES32.glUseProgram(shaderProgramObject);

        // transformations
        float[] modelViewMatrix = new float[16];
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -4.0f);

        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelViewProjectionMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0); // order of multiplication is very important

        // push above mvp into vertex shaders mvp uniform
        GLES32.glUniformMatrix4fv(mvpMatrixUniform, 1, false, modelViewProjectionMatrix, 0);

        final float[] colors = {1.0f, 1.0f, 0.0f, 1.0f};
        GLES32.glUniform4fv(colorUniform, 1, colors, 0);
        GLES32.glUniform1i(rectangleXSubdivisionUniform, rectangleXSubdivisions);
        //noinspection SuspiciousNameCombination
        GLES32.glUniform1i(rectangleYSubdivisionUniform, rectangleYSubdivisions);
        GLES32.glUniform1i(edge1SubdivisionUniform, edge1Subdivisions);
        GLES32.glUniform1i(edge2SubdivisionUniform, edge2Subdivisions);
        GLES32.glUniform1i(edge3SubdivisionUniform, edge3Subdivisions);
        GLES32.glUniform1i(edge4SubdivisionUniform, edge4Subdivisions);

        // Tell OpenGL by how many vertices one patch is created
        GLES32.glPatchParameteri(GLES32.GL_PATCH_VERTICES, 4);

        GLES32.glBindVertexArray(vao_triangle[0]);
        GLES32.glDrawArrays(GLES32.GL_PATCHES, 0, 4);
        GLES32.glBindVertexArray(0);

        GLES32.glUseProgram(0);

        // render
        requestRender();
    }

    /**
     * @noinspection EmptyMethod
     */
    private void update() {
    }

    public void uninitialize() {
        // code
        if (shaderProgramObject > 0) {
            GLES32.glUseProgram(shaderProgramObject);
            int[] retVal = new int[1];
            GLES32.glGetProgramiv(shaderProgramObject, GLES32.GL_ATTACHED_SHADERS, retVal, 0);
            int numShaders = retVal[0];
            if (numShaders > 0) {
                int[] pShaders = new int[numShaders];
                GLES32.glGetAttachedShaders(shaderProgramObject, numShaders, retVal, 0, pShaders, 0);
                for (int i = 0; i < numShaders; i++) {
                    GLES32.glDetachShader(shaderProgramObject, pShaders[i]);
                    GLES32.glDeleteShader(pShaders[i]);
                    pShaders[i] = 0;
                }
            }
            GLES32.glUseProgram(0);
            GLES32.glDeleteProgram(shaderProgramObject);
            shaderProgramObject = 0;
        }

        // triangle
        if (vbo_position_triangle[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position_triangle, 0);
            vbo_position_triangle[0] = 0;
        }
        if (vao_triangle[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao_triangle, 0);
            vao_triangle[0] = 0;
        }
    }
}
