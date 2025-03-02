package com.vnd.rtr5.x13_render_to_texture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.rtr5.common.Light;
import com.vnd.rtr5.common.Sphere;
import com.vnd.rtr5.common.VertexAttributesEnum;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLESView extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final int FBO_WIDTH = 512;
    private final int FBO_HEIGHT = 512;
    private final float[] materialAmbient_sphere = {0.0f, 0.0f, 0.0f, 1.0f};
    private final float[] materialDiffused_sphere = {1.0f, 1.0f, 1.0f, 1.0f};
    private final float[] materialSpecular_sphere = {1.0f, 1.0f, 1.0f, 1.0f};
    private final int[] vbo_position_cube = new int[1];
    private final int[] vbo_texture_coordinates_cube = new int[1];
    private final int[] vao_cube = new int[1];
    private final float[] perspectiveProjectionMatrix_cube = new float[16];
    private final int[] FBO = new int[1];
    private final int[] RBO = new int[1];
    private final int[] texture_FBO = new int[1];
    private final int[] vbo_position_sphere = new int[1];
    private final int[] vbo_normal_sphere = new int[1];
    private final int[] vao_sphere = new int[1];
    private final int[] lightAmbientUniform_sphere = new int[3];
    private final int[] lightDiffusedUniform_sphere = new int[3];
    private final int[] lightSpecularUniform_sphere = new int[3];
    private final int[] lightPositionUniform_sphere = new int[3];
    private final float[] perspectiveProjectionMatrix_sphere = new float[16];
    private final int[] vbo_element_sphere = new int[1];
    private final Light[] light = new Light[3];
    private final float[] lightAngle = new float[3];
    boolean bFBOResult = false;
    boolean vertexShaderEnabled = false;
    private GestureDetector gestureDetector = null;
    // no unsigned int in java
    private int shaderProgramObject_cube = 0;
    private int mvpMatrixUniform_cube;
    private float cAngle = 0.0f;
    private int textureSamplerUniform;
    private int winWidth = 0;
    private int winHeight = 0;
    private int shaderProgramObject_pv_sphere = 0;
    private int shaderProgramObject_pf_sphere = 0;
    private boolean bLightingEnabled = false;
    private int numElements;
    private int singleTap = 0;

    public GLESView(Context _context) {
        super(_context);

        // OpenGL ES related
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // when invalidate rect on windows

        // event related
        // create and set gestureDetector object
        gestureDetector = new GestureDetector(_context, this, null, false);
        gestureDetector.setOnDoubleTapListener(this);
    }

    // implementation of 3 methods of GLSurfaceView.renderer interface
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initialize_cube(gl);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        resize_cube(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        display_cube();
        update_cube();
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
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        singleTap = (singleTap + 1) % 3;
        if (singleTap == 1) {
            bLightingEnabled = true;
            vertexShaderEnabled = true;
        } else if (singleTap == 2) {
            bLightingEnabled = true;
            vertexShaderEnabled = false;
        } else {
            bLightingEnabled = false;
        }
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
    private void initialize_cube(GL10 gl) {
        // code
        printGLInfo(gl);

        // vertex shader
        final String vertexShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "in vec4 aPosition;" +
                        "uniform mat4 uMVPMatrix;" +
                        "in vec2 aTextureCoordinates;" +
                        "out vec2 oTextureCoordinates;" +
                        "void main(void)" +
                        "{" +
                        "gl_Position = uMVPMatrix * aPosition;" +
                        "oTextureCoordinates = aTextureCoordinates;" +
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

        // fragment shader
        final String fragmentShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "in vec2 oTextureCoordinates;" +
                        "uniform highp sampler2D uTextureSampler;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = texture(uTextureSampler, oTextureCoordinates);" +
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
        shaderProgramObject_cube = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject_cube, vertexShaderObject);
        GLES32.glAttachShader(shaderProgramObject_cube, fragmentShaderObject);
        GLES32.glBindAttribLocation(shaderProgramObject_cube, VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, "aPosition");
        GLES32.glBindAttribLocation(shaderProgramObject_cube, VertexAttributesEnum.AMC_ATTRIBUTE_TEXTURE_COORDINATES, "aTextureCoordinates");
        GLES32.glLinkProgram(shaderProgramObject_cube);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetProgramiv(shaderProgramObject_cube, GLES32.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetProgramiv(shaderProgramObject_cube, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject_cube);
                System.out.println("VND: shader program linking error log: " + szInfoLog);
            }
            uninitialize();
        }

        // get shader uniform locations - must be after linkage
        mvpMatrixUniform_cube = GLES32.glGetUniformLocation(shaderProgramObject_cube, "uMVPMatrix");
        textureSamplerUniform = GLES32.glGetUniformLocation(shaderProgramObject_cube, "uTextureSampler");

        final float[] cube_position = {
                // front
                1.0f, 1.0f, 1.0f, // top-right of front
                -1.0f, 1.0f, 1.0f, // top-left of front
                -1.0f, -1.0f, 1.0f, // bottom-left of front
                1.0f, -1.0f, 1.0f, // bottom-right of front

                // right
                1.0f, 1.0f, -1.0f, // top-right of right
                1.0f, 1.0f, 1.0f, // top-left of right
                1.0f, -1.0f, 1.0f, // bottom-left of right
                1.0f, -1.0f, -1.0f, // bottom-right of right

                // back
                1.0f, 1.0f, -1.0f, // top-right of back
                -1.0f, 1.0f, -1.0f, // top-left of back
                -1.0f, -1.0f, -1.0f, // bottom-left of back
                1.0f, -1.0f, -1.0f, // bottom-right of back

                // left
                -1.0f, 1.0f, 1.0f, // top-right of left
                -1.0f, 1.0f, -1.0f, // top-left of left
                -1.0f, -1.0f, -1.0f, // bottom-left of left
                -1.0f, -1.0f, 1.0f, // bottom-right of left

                // top
                1.0f, 1.0f, -1.0f, // top-right of top
                -1.0f, 1.0f, -1.0f, // top-left of top
                -1.0f, 1.0f, 1.0f, // bottom-left of top
                1.0f, 1.0f, 1.0f, // bottom-right of top

                // bottom
                1.0f, -1.0f, 1.0f, // top-right of bottom
                -1.0f, -1.0f, 1.0f, // top-left of bottom
                -1.0f, -1.0f, -1.0f, // bottom-left of bottom
                1.0f, -1.0f, -1.0f, // bottom-right of bottom
        };
        final float[] cube_texture_coordinates = {
                // front
                1.0f, 1.0f, // top-right of front
                0.0f, 1.0f, // top-left of front
                0.0f, 0.0f, // bottom-left of front
                1.0f, 0.0f, // bottom-right of front

                // right
                1.0f, 1.0f, // top-right of right
                0.0f, 1.0f, // top-left of right
                0.0f, 0.0f, // bottom-left of right
                1.0f, 0.0f, // bottom-right of right

                // back
                1.0f, 1.0f, // top-right of back
                0.0f, 1.0f, // top-left of back
                0.0f, 0.0f, // bottom-left of back
                1.0f, 0.0f, // bottom-right of back

                // left
                1.0f, 1.0f, // top-right of left
                0.0f, 1.0f, // top-left of left
                0.0f, 0.0f, // bottom-left of left
                1.0f, 0.0f, // bottom-right of left

                // top
                1.0f, 1.0f, // top-right of top
                0.0f, 1.0f, // top-left of top
                0.0f, 0.0f, // bottom-left of top
                1.0f, 0.0f, // bottom-right of top

                // bottom
                1.0f, 1.0f, // top-right of bottom
                0.0f, 1.0f, // top-left of bottom
                0.0f, 0.0f, // bottom-left of bottom
                1.0f, 0.0f, // bottom-right of bottom
        };

        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao_cube, 0);
        GLES32.glBindVertexArray(vao_cube[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position_cube, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position_cube[0]);
        // prepare cube vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferCubePosition = ByteBuffer.allocateDirect(cube_position.length * 4);
        byteBufferCubePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferCubePosition = byteBufferCubePosition.asFloatBuffer();
        floatBufferCubePosition.put(cube_position);
        floatBufferCubePosition.position(0);
        // now use
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, cube_position.length * 4, floatBufferCubePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // vbo for texture_coordinates - vertex buffer object
        GLES32.glGenBuffers(1, vbo_texture_coordinates_cube, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_texture_coordinates_cube[0]);
        // prepare cube vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferCubeTextureCoordinates = ByteBuffer.allocateDirect(cube_texture_coordinates.length * 4);
        byteBufferCubeTextureCoordinates.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferCubeTextureCoordinates = byteBufferCubeTextureCoordinates.asFloatBuffer();
        floatBufferCubeTextureCoordinates.put(cube_texture_coordinates);
        floatBufferCubeTextureCoordinates.position(0);
        // now use
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, cube_texture_coordinates.length * 4, floatBufferCubeTextureCoordinates, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_TEXTURE_COORDINATES, 2, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_TEXTURE_COORDINATES);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // Disable culling
        GLES32.glDisable(GLES32.GL_CULL_FACE);

        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Tell OpenGL to enable texture
        GLES32.glEnable(GLES32.GL_TEXTURE_2D);

        // initialize_cube perspectiveProjectionMatrix_cube
        Matrix.setIdentityM(perspectiveProjectionMatrix_cube, 0);

        // FBO related code
        if (createFBO()) {
            bFBOResult = initialize_sphere(gl);
        }
    }

    private boolean initialize_sphere(GL10 ignoredGl) {
        // code
        // vertex shader
        final String vertexShaderSourceCode_pv =
                "#version 320 es" +
                        "\n" +
                        "precision mediump int;" +
                        "in vec4 aPosition;" +
                        "in vec3 aNormal;" +
                        "uniform vec3 uLightAmbient[3];" +
                        "uniform vec3 uLightDiffused[3];" +
                        "uniform vec3 uLightSpecular[3];" +
                        "uniform vec4 uLightPositionMatrix[3];" +
                        "uniform vec3 uMaterialAmbient_sphere;" +
                        "uniform vec3 uMaterialDiffused_sphere;" +
                        "uniform vec3 uMaterialSpecular_sphere;" +
                        "uniform float uMaterialShininess_sphere;" +
                        "uniform int uKeyPress;" +
                        "uniform mat4 uModelMatrix;" +
                        "uniform mat4 uViewMatrix;" +
                        "uniform mat4 uProjectionMatrix;" +
                        "out vec3 oPhong_ADS_Light;" +
                        "void main(void)" +
                        "{" +
                        "oPhong_ADS_Light = vec3(0.0f, 0.0f, 0.0f);" +
                        "if (uKeyPress == 1)" +
                        "{" +
                        "vec3 lightDirection[3];" +
                        "vec3 reflectionVector[3];" +
                        "vec3 ambientLight[3];" +
                        "vec3 diffusedLight[3];" +
                        "vec3 lightSpecular[3];" +
                        "vec4 eyeCoordinates = uViewMatrix * uModelMatrix * aPosition;" +
                        "vec3 transformedNormals = normalize(mat3(uViewMatrix * uModelMatrix) * aNormal);" +
                        "vec3 viewerVector = normalize(-eyeCoordinates.xyz);" +
                        "for (int i = 0; i < 3; i++) " +
                        "{" +
                        "lightDirection[i] = normalize(vec3(uLightPositionMatrix[i] - eyeCoordinates));" +
                        "reflectionVector[i] = reflect(-lightDirection[i], transformedNormals);" +
                        "ambientLight[i] = uLightAmbient[i] * uMaterialAmbient_sphere;" +
                        "diffusedLight[i] = uLightDiffused[i] * uMaterialDiffused_sphere * max(dot(lightDirection[i], transformedNormals), 0.0f);" +
                        "lightSpecular[i] = uLightSpecular[i] * uMaterialSpecular_sphere * pow(max(dot(reflectionVector[i], viewerVector), 0.0f), uMaterialShininess_sphere);" +
                        "oPhong_ADS_Light = oPhong_ADS_Light + ambientLight[i] + diffusedLight[i] + lightSpecular[i];" +
                        "}" +
                        "}" +
                        "gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * aPosition;" +
                        "}";
        int vertexShaderObject_pv = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
        GLES32.glShaderSource(vertexShaderObject_pv, vertexShaderSourceCode_pv);
        GLES32.glCompileShader(vertexShaderObject_pv);
        int[] status = new int[1];
        int[] infoLogLength = new int[1];
        String szInfoLog;
        GLES32.glGetShaderiv(vertexShaderObject_pv, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(vertexShaderObject_pv, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(vertexShaderObject_pv);
                System.out.println("VND: vertex shader compilation error log: " + szInfoLog);
            }
            uninitialize_sphere();
            return false;
        }

        // fragment shader
        final String fragmentShaderSourceCode_pv =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "in vec3 oPhong_ADS_Light;" +
                        "uniform int uKeyPress;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "if (uKeyPress == 1)" +
                        "{" +
                        "fragColor = vec4(oPhong_ADS_Light, 1.0f);" +
                        "}" +
                        "else" +
                        "{" +
                        "fragColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);" +
                        "}" +
                        "}";
        int fragmentShaderObject_pv = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
        GLES32.glShaderSource(fragmentShaderObject_pv, fragmentShaderSourceCode_pv);
        GLES32.glCompileShader(fragmentShaderObject_pv);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(fragmentShaderObject_pv, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(fragmentShaderObject_pv, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(fragmentShaderObject_pv);
                System.out.println("VND: fragment shader compilation error log: " + szInfoLog);
            }
            uninitialize_sphere();
            return false;
        }

        // Shader program
        shaderProgramObject_pv_sphere = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject_pv_sphere, vertexShaderObject_pv);
        GLES32.glAttachShader(shaderProgramObject_pv_sphere, fragmentShaderObject_pv);
        GLES32.glBindAttribLocation(shaderProgramObject_pv_sphere, VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, "aPosition");
        GLES32.glBindAttribLocation(shaderProgramObject_pv_sphere, VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL, "aNormal");
        GLES32.glLinkProgram(shaderProgramObject_pv_sphere);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetProgramiv(shaderProgramObject_pv_sphere, GLES32.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetProgramiv(shaderProgramObject_pv_sphere, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject_pv_sphere);
                System.out.println("VND: shader program linking error log: " + szInfoLog);
            }
            uninitialize_sphere();
            return false;
        }

        // vertex shader
        final String vertexShaderSourceCode_pf =
                "#version 320 es" +
                        "\n" +
                        "precision mediump int;" +
                        "in vec4 aPosition;" +
                        "in vec3 aNormal;" +
                        "uniform int uKeyPress;" +
                        "uniform mat4 uModelMatrix;" +
                        "uniform mat4 uViewMatrix;" +
                        "uniform mat4 uProjectionMatrix;" +
                        "uniform vec4 uLightPositionMatrix[3];" +
                        "out vec3 oTransformedNormals;" +
                        "out vec3 oLightDirection[3];" +
                        "out vec3 oViewerVector;" +
                        "void main(void)" +
                        "{" +
                        "if (uKeyPress == 1)" +
                        "{" +
                        "vec4 eyeCoordinates = uViewMatrix * uModelMatrix * aPosition;" +
                        "oTransformedNormals = mat3(uViewMatrix * uModelMatrix) * aNormal;" +
                        "oLightDirection[0] = vec3(uLightPositionMatrix[0] - eyeCoordinates);" +
                        "oLightDirection[1] = vec3(uLightPositionMatrix[1] - eyeCoordinates);" +
                        "oLightDirection[2] = vec3(uLightPositionMatrix[2] - eyeCoordinates);" +
                        "oViewerVector = -eyeCoordinates.xyz;" +
                        "}" +
                        "else" +
                        "{" +
                        "oTransformedNormals = vec3(0.0f, 0.0f, 0.0f);" +
                        "oLightDirection[0] = vec3(0.0f, 0.0f, 0.0f);" +
                        "oLightDirection[1] = vec3(0.0f, 0.0f, 0.0f);" +
                        "oLightDirection[2] = vec3(0.0f, 0.0f, 0.0f);" +
                        "oViewerVector = vec3(0.0f, 0.0f, 0.0f);" +
                        "}" +
                        "gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * aPosition;" +
                        "}";
        int vertexShaderObject_pf = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
        GLES32.glShaderSource(vertexShaderObject_pf, vertexShaderSourceCode_pf);
        GLES32.glCompileShader(vertexShaderObject_pf);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(vertexShaderObject_pf, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(vertexShaderObject_pf, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(vertexShaderObject_pf);
                System.out.println("VND: vertex shader compilation error log: " + szInfoLog);
            }
            uninitialize_sphere();
            return false;
        }

        // fragment shader
        final String fragmentShaderSourceCode_pf =
                "#version 320 es" +
                        "\n" +
                        "precision highp float;" +
                        "in vec3 oTransformedNormals;" +
                        "in vec3 oLightDirection[3];" +
                        "in vec3 oViewerVector;" +
                        "uniform vec3 uLightAmbient[3];" +
                        "uniform vec3 uLightDiffused[3];" +
                        "uniform vec3 uLightSpecular[3];" +
                        "uniform vec3 uMaterialAmbient_sphere;" +
                        "uniform vec3 uMaterialDiffused_sphere;" +
                        "uniform vec3 uMaterialSpecular_sphere;" +
                        "uniform float uMaterialShininess_sphere;" +
                        "uniform int uKeyPress;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "vec3 phong_ADS_Light = vec3(0.0f, 0.0f, 0.0f);" +
                        "if (uKeyPress == 1)" +
                        "{" +
                        "vec3 reflectionVector[3];" +
                        "vec3 ambientLight[3];" +
                        "vec3 diffusedLight[3];" +
                        "vec3 lightSpecular[3];" +
                        "vec3 normalizedLightDirection[3];" +
                        "vec3 normalizedTransformedNormals = normalize(oTransformedNormals);" +
                        "vec3 normalizedViewerVector = normalize(oViewerVector);" +
                        "for (int i = 0; i < 3; i++)" +
                        "{" +
                        "normalizedLightDirection[i] = normalize(oLightDirection[i]);" +
                        "ambientLight[i] = uLightAmbient[i] * uMaterialAmbient_sphere;" +
                        "diffusedLight[i] = uLightDiffused[i] * uMaterialDiffused_sphere * max(dot(normalizedLightDirection[i], normalizedTransformedNormals), 0.0f);" +
                        "reflectionVector[i] = reflect(-normalizedLightDirection[i], normalizedTransformedNormals);" +
                        "lightSpecular[i] = uLightSpecular[i] * uMaterialSpecular_sphere * pow(max(dot(reflectionVector[i], normalizedViewerVector), 0.0f), uMaterialShininess_sphere);" +
                        "phong_ADS_Light = phong_ADS_Light + ambientLight[i] + diffusedLight[i] + lightSpecular[i];" +
                        "}" +
                        "fragColor = vec4(phong_ADS_Light, 1.0f);" +
                        "}" +
                        "else" +
                        "{" +
                        "fragColor = vec4(1.0f, 1.0f, 1.0, 1.0f);" +
                        "}" +
                        "}";
        int fragmentShaderObject_pf = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
        GLES32.glShaderSource(fragmentShaderObject_pf, fragmentShaderSourceCode_pf);
        GLES32.glCompileShader(fragmentShaderObject_pf);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetShaderiv(fragmentShaderObject_pf, GLES32.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetShaderiv(fragmentShaderObject_pf, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetShaderInfoLog(fragmentShaderObject_pf);
                System.out.println("VND: fragment shader compilation error log: " + szInfoLog);
            }
            uninitialize_sphere();
            return false;
        }

        // Shader program
        shaderProgramObject_pf_sphere = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject_pf_sphere, vertexShaderObject_pf);
        GLES32.glAttachShader(shaderProgramObject_pf_sphere, fragmentShaderObject_pf);
        GLES32.glBindAttribLocation(shaderProgramObject_pf_sphere, VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, "aPosition");
        GLES32.glBindAttribLocation(shaderProgramObject_pf_sphere, VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL, "aNormal");
        GLES32.glLinkProgram(shaderProgramObject_pf_sphere);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetProgramiv(shaderProgramObject_pf_sphere, GLES32.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetProgramiv(shaderProgramObject_pf_sphere, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject_pf_sphere);
                System.out.println("VND: shader program linking error log: " + szInfoLog);
            }
            uninitialize_sphere();
            return false;
        }

        Sphere sphere = new Sphere();
        float[] sphere_position = new float[1146];
        float[] sphere_normal = new float[1146];
        float[] sphere_texture_coordinates = new float[764];
        short[] sphere_element = new short[2280];
        sphere.getSphereVertexData(sphere_position, sphere_normal, sphere_texture_coordinates, sphere_element);
        numElements = sphere.getNumberOfSphereElements();

        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao_sphere, 0);
        GLES32.glBindVertexArray(vao_sphere[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position_sphere, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position_sphere[0]);
        // prepare sphere vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferSpherePosition = ByteBuffer.allocateDirect(sphere_position.length * 4);
        byteBufferSpherePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferSpherePosition = byteBufferSpherePosition.asFloatBuffer();
        floatBufferSpherePosition.put(sphere_position);
        floatBufferSpherePosition.position(0);
        // now use
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, sphere_position.length * 4, floatBufferSpherePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // vbo for normal - vertex buffer object
        GLES32.glGenBuffers(1, vbo_normal_sphere, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_normal_sphere[0]);
        // prepare sphere vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferSphereNormal = ByteBuffer.allocateDirect(sphere_normal.length * 4);
        byteBufferSphereNormal.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferSphereNormal = byteBufferSphereNormal.asFloatBuffer();
        floatBufferSphereNormal.put(sphere_normal);
        floatBufferSphereNormal.position(0);
        // now use
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, sphere_normal.length * 4, floatBufferSphereNormal, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // vbo for elements - vertex buffer object
        GLES32.glGenBuffers(1, vbo_element_sphere, 0);
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, vbo_element_sphere[0]);
        // prepare sphere vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferSphereElements = ByteBuffer.allocateDirect(sphere_element.length * 2);
        byteBufferSphereElements.order(ByteOrder.nativeOrder());
        ShortBuffer shortBufferSphereElements = byteBufferSphereElements.asShortBuffer();
        shortBufferSphereElements.put(sphere_element);
        shortBufferSphereElements.position(0);
        // now use
        GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER, sphere_element.length * 2, shortBufferSphereElements, GLES32.GL_STATIC_DRAW);
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

        // Depth enable settings
        GLES32.glClearDepthf(1.0f);
        GLES32.glEnable(GLES32.GL_DEPTH_TEST);
        GLES32.glDepthFunc(GLES32.GL_LEQUAL);

        // Disable culling
        GLES32.glDisable(GLES32.GL_CULL_FACE);

        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // initialize_sphere perspectiveProjectionMatrix_sphere
        Matrix.setIdentityM(perspectiveProjectionMatrix_sphere, 0);

        light[0] = new Light();
        light[1] = new Light();
        light[2] = new Light();
        light[0].ambient[0] = 0.0f;
        light[0].ambient[1] = 0.0f;
        light[0].ambient[2] = 0.0f;
        light[1].ambient[0] = 0.0f;
        light[1].ambient[1] = 0.0f;
        light[1].ambient[2] = 0.0f;
        light[2].ambient[0] = 0.0f;
        light[2].ambient[1] = 0.0f;
        light[2].ambient[2] = 0.0f;
        light[0].diffused[0] = 1.0f;
        light[0].diffused[1] = 0.0f;
        light[0].diffused[2] = 0.0f;
        light[1].diffused[0] = 0.0f;
        light[1].diffused[1] = 1.0f;
        light[1].diffused[2] = 0.0f;
        light[2].diffused[0] = 0.0f;
        light[2].diffused[1] = 0.0f;
        light[2].diffused[2] = 1.0f;
        light[0].specular[0] = 1.0f;
        light[0].specular[1] = 0.0f;
        light[0].specular[2] = 0.0f;
        light[1].specular[0] = 0.0f;
        light[1].specular[1] = 1.0f;
        light[1].specular[2] = 0.0f;
        light[2].specular[0] = 0.0f;
        light[2].specular[1] = 0.0f;
        light[2].specular[2] = 1.0f;

        return true;
    }

    boolean createFBO() {
        // variable declarations
        int[] maxRenderBufferSize = new int[1];

        // check capacity of render buffer
        GLES32.glGetIntegerv(GLES32.GL_MAX_RENDERBUFFER_SIZE, maxRenderBufferSize, 0);
        if (maxRenderBufferSize[0] < FBO_WIDTH) {
            System.out.println("VND: Texture size overflow!\n");
            return false;
        }

        // create custom framebuffer
        GLES32.glGenFramebuffers(1, FBO, 0);
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, FBO[0]);

        // create texture for FBO in which we are going to render the sphere
        GLES32.glGenTextures(1, texture_FBO, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture_FBO[0]);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexImage2D(GLES32.GL_TEXTURE_2D, 0, GLES32.GL_RGB, FBO_WIDTH, FBO_HEIGHT, 0, GLES32.GL_RGB, GLES32.GL_UNSIGNED_SHORT_5_6_5, null);
        // attach above texture to framebuffer at color attachment 0
        GLES32.glFramebufferTexture2D(GLES32.GL_FRAMEBUFFER, GLES32.GL_COLOR_ATTACHMENT0, GLES32.GL_TEXTURE_2D, texture_FBO[0], 0);
        // now create render buffer to hold depth of custom FBO
        GLES32.glGenRenderbuffers(1, RBO, 0);
        GLES32.glBindRenderbuffer(GLES32.GL_RENDERBUFFER, RBO[0]);
        // set the storage of render buffer of texture size for depth
        GLES32.glRenderbufferStorage(GLES32.GL_RENDERBUFFER, GLES32.GL_DEPTH_COMPONENT16, FBO_WIDTH, FBO_HEIGHT);
        // attach above depth related FBO to depth attachment
        GLES32.glFramebufferRenderbuffer(GLES32.GL_FRAMEBUFFER, GLES32.GL_DEPTH_ATTACHMENT, GLES32.GL_RENDERBUFFER, RBO[0]);
        // check the framebuffer status
        if (GLES32.glCheckFramebufferStatus(GLES32.GL_FRAMEBUFFER) != GLES32.GL_FRAMEBUFFER_COMPLETE) {
            System.out.println("VND: Framebuffer creation status is not complete!\n");
            return false;
        }
        // unbind framebuffer
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0);

        return true;
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

    private void resize_cube(int width, int height) {
        // code
        if (height <= 0) {
            height = 1;
        }

        if (width <= 0) {
            width = 1;
        }

        winWidth = width;
        winHeight = height;

        // Viewport == binocular
        GLES32.glViewport(0, 0, width, height);

        // set perspective projection matrix
        Matrix.perspectiveM(perspectiveProjectionMatrix_cube, 0, 45.0f, ((float) width / (float) height), 0.1f, 100.0f);
    }

    private void resize_sphere(int width, int height) {
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
        Matrix.perspectiveM(perspectiveProjectionMatrix_sphere, 0, 45.0f, ((float) width / (float) height), 0.1f, 100.0f);
    }

    private void display_cube() {
        if (bFBOResult) {
            display_sphere();
            update_sphere();
        }

        // call resize again to compensate change by display
        resize_cube(winWidth, winHeight);

        // reset color
        GLES32.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        GLES32.glUseProgram(shaderProgramObject_cube);

        // transformations
        float[] modelViewMatrix = new float[16];
        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM(modelViewMatrix, 0, cAngle, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(modelViewMatrix, 0, cAngle, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(modelViewMatrix, 0, cAngle, 0.0f, 0.0f, 1.0f);

        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelViewProjectionMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix_cube, 0, modelViewMatrix, 0); // order of multiplication is very important

        // push above mvp into vertex shaders mvp uniform
        GLES32.glUniformMatrix4fv(mvpMatrixUniform_cube, 1, false, modelViewProjectionMatrix, 0);

        // bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture_FBO[0]);
        GLES32.glUniform1i(textureSamplerUniform, 0);

        GLES32.glBindVertexArray(vao_cube[0]);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_FAN, 0, 4);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_FAN, 4, 4);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_FAN, 8, 4);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_FAN, 12, 4);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_FAN, 16, 4);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_FAN, 20, 4);
        GLES32.glBindVertexArray(0);

        // unbind texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        GLES32.glUseProgram(0);

        // render
        requestRender();
    }

    private void display_sphere() {
        // bind with FBO
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, FBO[0]);

        // call resize sphere
        resize_sphere(FBO_WIDTH, FBO_HEIGHT);

        // set color black to compensate change done by display sphere
        GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // code
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

        int keyPressUniform_sphere;
        int materialShininessUniform_sphere;
        int materialSpecularUniform_sphere;
        int materialDiffusedUniform_sphere;
        int materialAmbientUniform_sphere;
        int projectionMatrixUniform_sphere;
        int viewMatrixUniform_sphere;
        int modelMatrixUniform_sphere;
        if (vertexShaderEnabled) {
            GLES32.glUseProgram(shaderProgramObject_pv_sphere);

            modelMatrixUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uModelMatrix");
            viewMatrixUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uViewMatrix");
            projectionMatrixUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uProjectionMatrix");
            lightAmbientUniform_sphere[0] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightAmbient[0]");
            lightDiffusedUniform_sphere[0] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightDiffused[0]");
            lightSpecularUniform_sphere[0] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightSpecular[0]");
            lightPositionUniform_sphere[0] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightPositionMatrix[0]");
            lightAmbientUniform_sphere[1] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightAmbient[1]");
            lightDiffusedUniform_sphere[1] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightDiffused[1]");
            lightSpecularUniform_sphere[1] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightSpecular[1]");
            lightPositionUniform_sphere[1] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightPositionMatrix[1]");
            lightAmbientUniform_sphere[2] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightAmbient[2]");
            lightDiffusedUniform_sphere[2] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightDiffused[2]");
            lightSpecularUniform_sphere[2] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightSpecular[2]");
            lightPositionUniform_sphere[2] = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uLightPositionMatrix[2]");
            materialAmbientUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uMaterialAmbient_sphere");
            materialDiffusedUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uMaterialDiffused_sphere");
            materialSpecularUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uMaterialSpecular_sphere");
            materialShininessUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uMaterialShininess_sphere");
            keyPressUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pv_sphere, "uKeyPress");

        } else {
            GLES32.glUseProgram(shaderProgramObject_pf_sphere);

            modelMatrixUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uModelMatrix");
            viewMatrixUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uViewMatrix");
            projectionMatrixUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uProjectionMatrix");
            lightAmbientUniform_sphere[0] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightAmbient[0]");
            lightDiffusedUniform_sphere[0] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightDiffused[0]");
            lightSpecularUniform_sphere[0] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightSpecular[0]");
            lightPositionUniform_sphere[0] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightPositionMatrix[0]");
            lightAmbientUniform_sphere[1] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightAmbient[1]");
            lightDiffusedUniform_sphere[1] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightDiffused[1]");
            lightSpecularUniform_sphere[1] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightSpecular[1]");
            lightPositionUniform_sphere[1] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightPositionMatrix[1]");
            lightAmbientUniform_sphere[2] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightAmbient[2]");
            lightDiffusedUniform_sphere[2] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightDiffused[2]");
            lightSpecularUniform_sphere[2] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightSpecular[2]");
            lightPositionUniform_sphere[2] = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uLightPositionMatrix[2]");
            materialAmbientUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uMaterialAmbient_sphere");
            materialDiffusedUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uMaterialDiffused_sphere");
            materialSpecularUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uMaterialSpecular_sphere");
            materialShininessUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uMaterialShininess_sphere");
            keyPressUniform_sphere = GLES32.glGetUniformLocation(shaderProgramObject_pf_sphere, "uKeyPress");
        }

        // sphere
        // transformations
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, -1.5f);

        float[] viewMatrix = new float[16];
        Matrix.setIdentityM(viewMatrix, 0);

        // push above mvp into vertex shaders mvp uniform
        GLES32.glUniformMatrix4fv(modelMatrixUniform_sphere, 1, false, modelMatrix, 0);
        GLES32.glUniformMatrix4fv(viewMatrixUniform_sphere, 1, false, viewMatrix, 0);
        GLES32.glUniformMatrix4fv(projectionMatrixUniform_sphere, 1, false, perspectiveProjectionMatrix_sphere, 0);

        if (bLightingEnabled) {
            GLES32.glUniform1i(keyPressUniform_sphere, 1);

            GLES32.glUniform3fv(lightAmbientUniform_sphere[0], 1, light[0].ambient, 0);
            GLES32.glUniform3fv(lightDiffusedUniform_sphere[0], 1, light[0].diffused, 0);
            GLES32.glUniform3fv(lightSpecularUniform_sphere[0], 1, light[0].specular, 0);
            GLES32.glUniform4fv(lightPositionUniform_sphere[0], 1, light[0].position, 0);
            GLES32.glUniform3fv(lightAmbientUniform_sphere[1], 1, light[1].ambient, 0);
            GLES32.glUniform3fv(lightDiffusedUniform_sphere[1], 1, light[1].diffused, 0);
            GLES32.glUniform3fv(lightSpecularUniform_sphere[1], 1, light[1].specular, 0);
            GLES32.glUniform4fv(lightPositionUniform_sphere[1], 1, light[1].position, 0);
            GLES32.glUniform3fv(lightAmbientUniform_sphere[2], 1, light[2].ambient, 0);
            GLES32.glUniform3fv(lightDiffusedUniform_sphere[2], 1, light[2].diffused, 0);
            GLES32.glUniform3fv(lightSpecularUniform_sphere[2], 1, light[2].specular, 0);
            GLES32.glUniform4fv(lightPositionUniform_sphere[2], 1, light[2].position, 0);
            GLES32.glUniform3fv(materialAmbientUniform_sphere, 1, materialAmbient_sphere, 0);
            GLES32.glUniform3fv(materialDiffusedUniform_sphere, 1, materialDiffused_sphere, 0);
            GLES32.glUniform3fv(materialSpecularUniform_sphere, 1, materialSpecular_sphere, 0);
            float materialShininess_sphere = 128.0f;
            GLES32.glUniform1f(materialShininessUniform_sphere, materialShininess_sphere);
        } else {
            GLES32.glUniform1i(keyPressUniform_sphere, 0);
        }

        // bind vao
        GLES32.glBindVertexArray(vao_sphere[0]);

        // *** draw, either by glDrawTriangles() or glDrawArrays() or glDrawElements()
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, vbo_element_sphere[0]);
        GLES32.glDrawElements(GLES32.GL_TRIANGLES, numElements, GLES32.GL_UNSIGNED_SHORT, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

        GLES32.glUseProgram(0);

        // unbind framebuffer
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0);

        // render
        requestRender();
    }

    private void update_cube() {
        cAngle -= 1.0f;
        if (cAngle <= 0.0f) {
            cAngle += 360.0f;
        }
    }

    private void update_sphere() {
        // animating lights
        lightAngle[0] = lightAngle[0] + 0.05f;
        if (lightAngle[0] > 360.0f) {
            lightAngle[0] = lightAngle[0] - 360.0f;
        }
        lightAngle[1] = lightAngle[1] + 0.05f;
        if (lightAngle[1] > 360.0f) {
            lightAngle[1] = lightAngle[1] - 360.0f;
        }
        lightAngle[2] = lightAngle[2] + 0.05f;
        if (lightAngle[2] > 360.0f) {
            lightAngle[2] = lightAngle[2] - 360.0f;
        }
        light[0].position[0] = 0.0f; // by rule
        light[0].position[1] = (float) (5.0f * Math.cos(lightAngle[0])); // one of index 1 and 2 should have value, so chosen to keep zero here
        light[0].position[2] = (float) (5.0f * Math.sin(lightAngle[0]));
        light[0].position[3] = 1.0f;
        light[1].position[0] = (float) (5.0f * Math.sin(lightAngle[1]));
        light[1].position[1] = 0.0f; // by rule
        light[1].position[2] = (float) (5.0f * Math.cos(lightAngle[1])); // one of index 0 and 2 should have value, so chosen to keep zero here
        light[1].position[3] = 1.0f;
        light[2].position[0] = (float) (5.0f * Math.cos(lightAngle[2])); // one of index 0 and 1 should have value, so chosen to keep zero here
        light[2].position[1] = (float) (5.0f * Math.sin(lightAngle[2]));
        light[2].position[2] = 0.0f; // by rule
        light[2].position[3] = 1.0f;
    }

    public void uninitialize() {
        // code
        uninitialize_sphere();
        if (texture_FBO[0] > 0) {
            GLES32.glDeleteTextures(1, texture_FBO, 0);
            texture_FBO[0] = 0;
        }
        if (RBO[0] > 0) {
            GLES32.glDeleteRenderbuffers(1, RBO, 0);
            RBO[0] = 0;
        }
        if (FBO[0] > 0) {
            GLES32.glDeleteRenderbuffers(1, FBO, 0);
            FBO[0] = 0;
        }
        if (shaderProgramObject_cube > 0) {
            GLES32.glUseProgram(shaderProgramObject_cube);
            int[] retVal = new int[1];
            GLES32.glGetProgramiv(shaderProgramObject_cube, GLES32.GL_ATTACHED_SHADERS, retVal, 0);
            int numShaders = retVal[0];
            if (numShaders > 0) {
                int[] pShaders = new int[numShaders];
                GLES32.glGetAttachedShaders(shaderProgramObject_cube, numShaders, retVal, 0, pShaders, 0);
                for (int i = 0; i < numShaders; i++) {
                    GLES32.glDetachShader(shaderProgramObject_cube, pShaders[i]);
                    GLES32.glDeleteShader(pShaders[i]);
                    pShaders[i] = 0;
                }
            }
            GLES32.glUseProgram(0);
            GLES32.glDeleteProgram(shaderProgramObject_cube);
            shaderProgramObject_cube = 0;
        }

        // cube
        if (vbo_texture_coordinates_cube[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_texture_coordinates_cube, 0);
            vbo_texture_coordinates_cube[0] = 0;
        }
        if (vbo_position_cube[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position_cube, 0);
            vbo_position_cube[0] = 0;
        }
        if (vao_cube[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao_cube, 0);
            vao_cube[0] = 0;
        }
    }

    public void uninitialize_sphere() {
        // code
        if (shaderProgramObject_pv_sphere > 0) {
            GLES32.glUseProgram(shaderProgramObject_pv_sphere);
            int[] retVal = new int[1];
            GLES32.glGetProgramiv(shaderProgramObject_pv_sphere, GLES32.GL_ATTACHED_SHADERS, retVal, 0);
            int numShaders = retVal[0];
            if (numShaders > 0) {
                int[] pShaders = new int[numShaders];
                GLES32.glGetAttachedShaders(shaderProgramObject_pv_sphere, numShaders, retVal, 0, pShaders, 0);
                for (int i = 0; i < numShaders; i++) {
                    GLES32.glDetachShader(shaderProgramObject_pv_sphere, pShaders[i]);
                    GLES32.glDeleteShader(pShaders[i]);
                    pShaders[i] = 0;
                }
            }
            GLES32.glUseProgram(0);
            GLES32.glDeleteProgram(shaderProgramObject_pv_sphere);
            shaderProgramObject_pv_sphere = 0;
        }
        if (shaderProgramObject_pf_sphere > 0) {
            GLES32.glUseProgram(shaderProgramObject_pf_sphere);
            int[] retVal = new int[1];
            GLES32.glGetProgramiv(shaderProgramObject_pf_sphere, GLES32.GL_ATTACHED_SHADERS, retVal, 0);
            int numShaders = retVal[0];
            if (numShaders > 0) {
                int[] pShaders = new int[numShaders];
                GLES32.glGetAttachedShaders(shaderProgramObject_pf_sphere, numShaders, retVal, 0, pShaders, 0);
                for (int i = 0; i < numShaders; i++) {
                    GLES32.glDetachShader(shaderProgramObject_pf_sphere, pShaders[i]);
                    GLES32.glDeleteShader(pShaders[i]);
                    pShaders[i] = 0;
                }
            }
            GLES32.glUseProgram(0);
            GLES32.glDeleteProgram(shaderProgramObject_pf_sphere);
            shaderProgramObject_pf_sphere = 0;
        }

        // sphere
        if (vbo_normal_sphere[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_normal_sphere, 0);
            vbo_normal_sphere[0] = 0;
        }
        if (vbo_position_sphere[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position_sphere, 0);
            vbo_position_sphere[0] = 0;
        }
        if (vbo_element_sphere[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_element_sphere, 0);
            vbo_element_sphere[0] = 0;
        }
        if (vao_sphere[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao_sphere, 0);
            vao_sphere[0] = 0;
        }
    }
}

