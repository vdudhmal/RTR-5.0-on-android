package com.vnd.rtr5.x10_lights.x7_24spheres.x2_per_fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.rtr5.common.Sphere;
import com.vnd.rtr5.common.VertexAttributesEnum;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLESView extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] lightAmbient = {0.0f, 0.0f, 0.0f, 1.0f}; // ambient light
    private final float[] lightDiffused = {1.0f, 1.0f, 1.0f, 1.0f}; // white diffused light
    private final float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f}; // white specular light
    private final float[] lightPosition = {0.0f, 0.0f, 0.0f, 1.0f};
    // Material property
    private final float[][][] materialAmbient = {
            {{0.0215f, 0.1745f, 0.0215f, 1.0f}, {0.329412f, 0.223529f, 0.027451f, 1.0f}, {0.0f, 0.0f, 0.0f, 1.0f}, {0.02f, 0.02f, 0.02f, 1.0f}},
            {{0.135f, 0.2225f, 0.1575f, 1.0f}, {0.2125f, 0.1275f, 0.054f, 1.0f}, {0.0f, 0.1f, 0.06f, 1.0f}, {0.0f, 0.05f, 0.05f, 1.0f}},
            {{0.05375f, 0.05f, 0.06625f, 1.0f}, {0.25f, 0.25f, 0.25f, 1.0f}, {0.0f, 0.0f, 0.0f, 1.0f}, {0.0f, 0.05f, 0.0f, 1.0f}},
            {{0.25f, 0.20725f, 0.20725f, 1.0f}, {0.19125f, 0.0735f, 0.0225f, 1.0f}, {0.0f, 0.0f, 0.0f, 1.0f}, {0.05f, 0.0f, 0.0f, 1.0f}},
            {{0.1745f, 0.01175f, 0.01175f, 1.0f}, {0.24725f, 0.1995f, 0.0745f, 1.0f}, {0.0f, 0.0f, 0.0f, 1.0f}, {0.05f, 0.05f, 0.05f, 1.0f}},
            {{0.1f, 0.18725f, 0.1745f, 1.0f}, {0.19225f, 0.19225f, 0.19225f, 1.0f}, {0.0f, 0.0f, 0.0f, 1.0f}, {0.05f, 0.05f, 0.0f, 1.0f}}
    };
    private final float[][][] materialDiffused = {
            {{0.07568f, 0.61424f, 0.07568f, 1.0f}, {0.780392f, 0.568627f, 0.113725f, 1.0f}, {0.01f, 0.01f, 0.01f, 1.0f}, {0.01f, 0.01f, 0.01f, 1.0f}},
            {{0.54f, 0.89f, 0.63f, 1.0f}, {0.714f, 0.4284f, 0.18144f, 1.0f}, {0.0f, 0.50980392f, 0.50980392f, 1.0f}, {0.4f, 0.5f, 0.5f, 1.0f}},
            {{0.18275f, 0.17f, 0.22525f, 1.0f}, {0.4f, 0.4f, 0.4f, 1.0f}, {0.1f, 0.35f, 0.1f, 1.0f}, {0.4f, 0.5f, 0.4f, 1.0f}},
            {{1.0f, 0.829f, 0.829f, 1.0f}, {0.7038f, 0.27048f, 0.0828f, 1.0f}, {0.5f, 0.0f, 0.0f, 1.0f}, {0.5f, 0.4f, 0.4f, 1.0f}},
            {{0.61424f, 0.04136f, 0.04136f, 1.0f}, {0.75164f, 0.60648f, 0.22648f, 1.0f}, {0.55f, 0.55f, 0.55f, 1.0f}, {0.5f, 0.5f, 0.5f, 1.0f}},
            {{0.396f, 0.74151f, 0.69102f, 1.0f}, {0.50754f, 0.50754f, 0.50754f, 1.0f}, {0.5f, 0.5f, 0.0f, 1.0f}, {0.5f, 0.5f, 0.4f, 1.0f}}
    };
    private final float[][][] materialSpecular = {
            {{0.633f, 0.727811f, 0.633f, 1.0f}, {0.992157f, 0.941176f, 0.807843f, 1.0f}, {0.5f, 0.5f, 0.5f, 1.0f}, {0.4f, 0.4f, 0.4f, 1.0f}},
            {{0.316228f, 0.316228f, 0.316228f, 1.0f}, {0.393548f, 0.271906f, 0.166721f, 1.0f}, {0.50196078f, 0.50196078f, 0.50196078f, 1.0f}, {0.04f, 0.7f, 0.7f, 1.0f}},
            {{0.332741f, 0.328634f, 0.346435f, 1.0f}, {0.774597f, 0.774597f, 0.774597f, 1.0f}, {0.45f, 0.55f, 0.45f, 1.0f}, {0.04f, 0.7f, 0.04f, 1.0f}},
            {{0.296648f, 0.296648f, 0.296648f, 1.0f}, {0.256777f, 0.137622f, 0.086014f, 1.0f}, {0.7f, 0.6f, 0.6f, 1.0f}, {0.7f, 0.04f, 0.04f, 1.0f}},
            {{0.727811f, 0.626959f, 0.626959f, 1.0f}, {0.628281f, 0.555802f, 0.366065f, 1.0f}, {0.7f, 0.7f, 0.7f, 1.0f}, {0.7f, 0.7f, 0.7f, 1.0f}},
            {{0.297254f, 0.30829f, 0.306678f, 1.0f}, {0.508273f, 0.508273f, 0.508273f, 1.0f}, {0.6f, 0.6f, 0.5f, 1.0f}, {0.7f, 0.7f, 0.04f, 1.0f}}
    };
    private final float[][] materialShininess = {
            {0.6f * 128.0f, 0.21794872f * 128.0f, 0.25f * 128.0f, 0.078125f * 128.0f},
            {0.1f * 128.0f, 0.2f * 128.0f, 0.25f * 128.0f, 0.078125f * 128.0f},
            {0.3f * 128.0f, 0.6f * 128.0f, 0.25f * 128.0f, 0.078125f * 128.0f},
            {0.088f * 128.0f, 0.1f * 128.0f, 0.25f * 128.0f, 0.078125f * 128.0f},
            {0.6f * 128.0f, 0.4f * 128.0f, 0.25f * 128.0f, 0.078125f * 128.0f},
            {0.1f * 128.0f, 0.4f * 128.0f, 0.25f * 128.0f, 0.078125f * 128.0f}
    };
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] vbo_element_sphere = new int[1];
    private final int[] vbo_position_sphere = new int[1];
    private final int[] vbo_normal_sphere = new int[1];
    private final int[] vao_sphere = new int[1];
    float lightAngleX = 0.0f;
    float lightAngleY = 0.0f;
    float lightAngleZ = 0.0f;
    boolean bXRotationEnabled = false;
    boolean bYRotationEnabled = false;
    boolean bZRotationEnabled = false;
    private GestureDetector gestureDetector = null;
    // no unsigned int in java
    private int shaderProgramObject = 0;
    private int modelMatrixUniform = 0;
    private int viewMatrixUniform = 0;
    private int projectionMatrixUniform = 0;
    private int lightAmbientUniform = 0;
    private int lightDiffusedUniform = 0;
    private int lightSpecularUniform = 0;
    private int lightPositionUniform = 0;
    private int materialAmbientUniform = 0;
    private int materialDiffusedUniform = 0;
    private int materialSpecularUniform = 0;
    private int materialShininessUniform = 0;
    private int keyPressUniform = 0;
    private boolean bLightingEnabled = true;
    private int numElements;
    private int singleTap = 0;

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
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        singleTap = (singleTap + 1) % 5;
        if (singleTap == 1) {
            bXRotationEnabled = true;
            bYRotationEnabled = false;
            bZRotationEnabled = false;
        } else if (singleTap == 2) {
            bXRotationEnabled = false;
            bYRotationEnabled = true;
            bZRotationEnabled = false;
        } else if (singleTap == 3) {
            bXRotationEnabled = false;
            bYRotationEnabled = false;
            bZRotationEnabled = true;
        } else if (singleTap == 4) {
            bLightingEnabled = false;
            bXRotationEnabled = false;
            bYRotationEnabled = false;
            bZRotationEnabled = false;
        } else {
            bLightingEnabled = true;
        }
        lightAngleX = 0.0f;
        lightAngleY = 0.0f;
        lightAngleZ = 0.0f;
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
    private void initialize(GL10 gl) {
        // code
        printGLInfo(gl);

        // vertex shader
        final String vertexShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision mediump int;" +
                        "in vec4 aPosition;" +
                        "in vec3 aNormal;" +
                        "uniform int uKeyPress;" +
                        "uniform mat4 uModelMatrix;" +
                        "uniform mat4 uViewMatrix;" +
                        "uniform mat4 uProjectionMatrix;" +
                        "uniform vec4 uLightPositionMatrix;" +
                        "out vec3 oTransformedNormals;" +
                        "out vec3 oLightDirection;" +
                        "out vec3 oViewerVector;" +
                        "void main(void)" +
                        "{" +
                        "if (uKeyPress == 1)" +
                        "{" +
                        "vec4 eyeCoordinates = uViewMatrix * uModelMatrix * aPosition;" +
                        "oTransformedNormals = mat3(uViewMatrix * uModelMatrix) * aNormal;" +
                        "oLightDirection = vec3(uLightPositionMatrix - eyeCoordinates);" +
                        "oViewerVector = -eyeCoordinates.xyz;" +
                        "}" +
                        "else" +
                        "{" +
                        "oTransformedNormals = vec3(0.0f, 0.0f, 0.0f);" +
                        "oLightDirection = vec3(0.0f, 0.0f, 0.0f);" +
                        "oViewerVector = vec3(0.0f, 0.0f, 0.0f);" +
                        "}" +
                        "gl_Position = uProjectionMatrix * uViewMatrix * uModelMatrix * aPosition;" +
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
                        "in vec3 oTransformedNormals;" +
                        "in vec3 oLightDirection;" +
                        "in vec3 oViewerVector;" +
                        "uniform vec3 uLightAmbient;" +
                        "uniform vec3 uLightDiffused;" +
                        "uniform vec3 uLightSpecular;" +
                        "uniform vec3 uMaterialAmbient;" +
                        "uniform vec3 uMaterialDiffused;" +
                        "uniform vec3 uMaterialSpecular;" +
                        "uniform float uMaterialShininess;" +
                        "uniform int uKeyPress;" +
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "if (uKeyPress == 1)" +
                        "{" +
                        "vec3 normalizedTransformedNormals = normalize(oTransformedNormals);" +
                        "vec3 normalizedLightDirection = normalize(oLightDirection);" +
                        "vec3 normalizedViewerVector = normalize(oViewerVector);" +
                        "vec3 ambientLight = uLightAmbient * uMaterialAmbient;" +
                        "vec3 diffusedLight = uLightDiffused * uMaterialDiffused * max(dot(normalizedLightDirection, normalizedTransformedNormals), 0.0f);" +
                        "vec3 reflectionVector = reflect(-normalizedLightDirection, normalizedTransformedNormals);" +
                        "vec3 lightSpecular = uLightSpecular * uMaterialSpecular * pow(max(dot(reflectionVector, normalizedViewerVector), 0.0f), uMaterialShininess);" +
                        "vec3 phong_ADS_Light = ambientLight + diffusedLight + lightSpecular;" +
                        "fragColor = vec4(phong_ADS_Light, 1.0f);" +
                        "}" +
                        "else" +
                        "{" +
                        "vec3 phong_ADS_Light = vec3(1.0f, 1.0f, 1.0f);" +
                        "fragColor = vec4(phong_ADS_Light, 1.0f);" +
                        "}" +
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
        GLES32.glAttachShader(shaderProgramObject, fragmentShaderObject);
        GLES32.glBindAttribLocation(shaderProgramObject, VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, "aPosition");
        GLES32.glBindAttribLocation(shaderProgramObject, VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL, "aNormal");
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

        modelMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uModelMatrix");
        viewMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uViewMatrix");
        projectionMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uProjectionMatrix");
        lightAmbientUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uLightAmbient");
        lightDiffusedUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uLightDiffused");
        lightSpecularUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uLightSpecular");
        lightPositionUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uLightPositionMatrix");
        materialAmbientUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uMaterialAmbient");
        materialDiffusedUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uMaterialDiffused");
        materialSpecularUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uMaterialSpecular");
        materialShininessUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uMaterialShininess");
        keyPressUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uKeyPress");

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

        // sphere
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 6; i++) {
                // transformations
                float[] modelMatrix = new float[16];
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, -6.0f + 4.0f * j, 3.0f - i * 1.25f, -10.0f);
                float[] viewMatrix = new float[16];
                Matrix.setIdentityM(viewMatrix, 0);

                // push above mvp into vertex shaders mvp uniform
                GLES32.glUniformMatrix4fv(modelMatrixUniform, 1, false, modelMatrix, 0);
                GLES32.glUniformMatrix4fv(viewMatrixUniform, 1, false, viewMatrix, 0);
                GLES32.glUniformMatrix4fv(projectionMatrixUniform, 1, false, perspectiveProjectionMatrix, 0);

                if (bLightingEnabled) {
                    GLES32.glUniform1i(keyPressUniform, 1);
                    GLES32.glUniform3fv(lightAmbientUniform, 1, lightAmbient, 0);
                    GLES32.glUniform3fv(lightDiffusedUniform, 1, lightDiffused, 0);
                    GLES32.glUniform3fv(lightSpecularUniform, 1, lightSpecular, 0);
                    GLES32.glUniform4fv(lightPositionUniform, 1, lightPosition, 0);
                    GLES32.glUniform3fv(materialAmbientUniform, 1, materialAmbient[i][j], 0);
                    GLES32.glUniform3fv(materialDiffusedUniform, 1, materialDiffused[i][j], 0);
                    GLES32.glUniform3fv(materialSpecularUniform, 1, materialSpecular[i][j], 0);
                    GLES32.glUniform1f(materialShininessUniform, materialShininess[i][j]);
                } else {
                    GLES32.glUniform1i(keyPressUniform, 0);
                }

                // bind vao
                GLES32.glBindVertexArray(vao_sphere[0]);

                // *** draw, either by glDrawTriangles() or glDrawArrays() or glDrawElements()
                GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, vbo_element_sphere[0]);
                GLES32.glDrawElements(GLES32.GL_TRIANGLES, numElements, GLES32.GL_UNSIGNED_SHORT, 0);

                // unbind vao
                GLES32.glBindVertexArray(0);
            }
        }

        GLES32.glUseProgram(0);

        // render
        requestRender();
    }

    private void update() {
        // animating lights
        if (bXRotationEnabled) {
            lightAngleX = lightAngleX + 0.125f;
            if (lightAngleX > 360.0f) {
                lightAngleX = lightAngleX - 360.0f;
            }
            lightPosition[0] = 0.0f; // by rule
            lightPosition[1] = (float) (45.0f * Math.cos(lightAngleX)); // one of index 1 and 2 should have value, so chosen to keep zero here
            lightPosition[2] = (float) (45.0f * Math.sin(lightAngleX));
            lightPosition[3] = 1.0f;
        } else if (bYRotationEnabled) {
            lightAngleY = lightAngleY + 0.125f;
            if (lightAngleY > 360.0f) {
                lightAngleY = lightAngleY - 360.0f;
            }
            lightPosition[0] = (float) (45.0f * Math.sin(lightAngleY));
            lightPosition[1] = 0.0f; // by rule
            lightPosition[2] = (float) (45.0f * Math.cos(lightAngleY)); // one of index 0 and 2 should have value, so chosen to keep zero here
            lightPosition[3] = 1.0f;
        } else if (bZRotationEnabled) {
            lightAngleZ = lightAngleZ + 0.125f;
            if (lightAngleZ > 360.0f) {
                lightAngleZ = lightAngleZ - 360.0f;
            }
            lightPosition[0] = (float) (45.0f * Math.cos(lightAngleZ)); // one of index 0 and 1 should have value, so chosen to keep zero here
            lightPosition[1] = (float) (45.0f * Math.sin(lightAngleZ));
            lightPosition[2] = 0.0f; // by rule
            lightPosition[3] = 1.0f;
        }
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

