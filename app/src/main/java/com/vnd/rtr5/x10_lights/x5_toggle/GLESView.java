package com.vnd.rtr5.x10_lights.x5_toggle;

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
    private final float[] lightAmbient = {0.1f, 0.1f, 0.1f, 1.0f}; // ambient light
    private final float[] lightDiffused = {1.0f, 1.0f, 1.0f, 1.0f}; // white diffused light
    private final float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f}; // white specular light
    private final float[] lightPosition = {100.0f, 100.0f, 100.0f, 1.0f};
    private final float[] materialAmbientAlbedo = {0.0f, 0.0f, 0.0f, 1.0f};
    private final float[] materialDiffusedAlbedo = {0.5f, 0.2f, 0.7f, 1.0f};
    private final float[] materialSpecularAlbedo = {0.7f, 0.7f, 0.7f, 1.0f};
    private final float[] materialAmbientWhite = {0.0f, 0.0f, 0.0f, 1.0f};
    private final float[] materialDiffusedWhite = {1.0f, 1.0f, 1.0f, 1.0f};
    private final float[] materialSpecularWhite = {1.0f, 1.0f, 1.0f, 1.0f};
    private final int[] vbo_position_sphere = new int[1];
    private final int[] vbo_normal_sphere = new int[1];
    private final int[] vao_sphere = new int[1];
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] vbo_element_sphere = new int[1];
    boolean vertexShaderEnabled = false;
    boolean whiteMaterial = false;
    private GestureDetector gestureDetector = null;
    // no unsigned int in java
    private int shaderProgramObject_pv = 0;
    private int shaderProgramObject_pf = 0;
    private boolean bLightingEnabled = false;
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
            bLightingEnabled = true;
            vertexShaderEnabled = true;
            whiteMaterial = true;
        } else if (singleTap == 2) {
            bLightingEnabled = true;
            vertexShaderEnabled = true;
            whiteMaterial = false;
        } else if (singleTap == 3) {
            bLightingEnabled = true;
            vertexShaderEnabled = false;
            whiteMaterial = true;
        } else if (singleTap == 4) {
            bLightingEnabled = true;
            vertexShaderEnabled = false;
            whiteMaterial = false;
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
    private void initialize(GL10 gl) {
        // code
        printGLInfo(gl);

        // vertex shader
        final String vertexShaderSourceCode_pv =
                "#version 320 es" +
                        "\n" +
                        "precision mediump int;" +
                        "in vec4 aPosition;" +
                        "in vec3 aNormal;" +
                        "uniform vec3 uLightAmbient;" +
                        "uniform vec3 uLightDiffused;" +
                        "uniform vec3 uLightSpecular;" +
                        "uniform vec4 uLightPositionMatrix;" +
                        "uniform vec3 uMaterialAmbient;" +
                        "uniform vec3 uMaterialDiffused;" +
                        "uniform vec3 uMaterialSpecular;" +
                        "uniform float uMaterialShininess;" +
                        "uniform int uKeyPress;" +
                        "uniform mat4 uModelMatrix;" +
                        "uniform mat4 uViewMatrix;" +
                        "uniform mat4 uProjectionMatrix;" +
                        "out vec3 oPhong_ADS_Light;" +
                        "void main(void)" +
                        "{" +
                        "if (uKeyPress == 1)" +
                        "{" +
                        "vec4 eyeCoordinates = uViewMatrix * uModelMatrix * aPosition;" +
                        "vec3 transformedNormals = normalize(mat3(uViewMatrix * uModelMatrix) * aNormal);" +
                        "vec3 lightDirection = normalize(vec3(uLightPositionMatrix - eyeCoordinates));" +
                        "vec3 reflectionVector = reflect(-lightDirection, transformedNormals);" +
                        "vec3 viewerVector = normalize(-eyeCoordinates.xyz);" +
                        "vec3 ambientLight = uLightAmbient * uMaterialAmbient;" +
                        "vec3 diffusedLight = uLightDiffused * uMaterialDiffused * max(dot(lightDirection, transformedNormals), 0.0f);" +
                        "vec3 lightSpecular = uLightSpecular * uMaterialSpecular * pow(max(dot(reflectionVector, viewerVector), 0.0f), uMaterialShininess);" +
                        "oPhong_ADS_Light = ambientLight + diffusedLight + lightSpecular;" +
                        "}" +
                        "else" +
                        "{" +
                        "oPhong_ADS_Light = vec3(0.0f, 0.0f, 0.0f);" +
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
            uninitialize();
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
            uninitialize();
        }

        // Shader program
        shaderProgramObject_pv = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject_pv, vertexShaderObject_pv);
        GLES32.glAttachShader(shaderProgramObject_pv, fragmentShaderObject_pv);
        GLES32.glBindAttribLocation(shaderProgramObject_pv, VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, "aPosition");
        GLES32.glBindAttribLocation(shaderProgramObject_pv, VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL, "aNormal");
        GLES32.glLinkProgram(shaderProgramObject_pv);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetProgramiv(shaderProgramObject_pv, GLES32.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetProgramiv(shaderProgramObject_pv, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject_pv);
                System.out.println("VND: shader program linking error log: " + szInfoLog);
            }
            uninitialize();
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
            uninitialize();
        }

        // fragment shader
        final String fragmentShaderSourceCode_pf =
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
            uninitialize();
        }

        // Shader program
        shaderProgramObject_pf = GLES32.glCreateProgram();
        GLES32.glAttachShader(shaderProgramObject_pf, vertexShaderObject_pf);
        GLES32.glAttachShader(shaderProgramObject_pf, fragmentShaderObject_pf);
        GLES32.glBindAttribLocation(shaderProgramObject_pf, VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, "aPosition");
        GLES32.glBindAttribLocation(shaderProgramObject_pf, VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL, "aNormal");
        GLES32.glLinkProgram(shaderProgramObject_pf);
        status[0] = 0;
        infoLogLength[0] = 0;
        GLES32.glGetProgramiv(shaderProgramObject_pf, GLES32.GL_LINK_STATUS, status, 0);
        if (status[0] == GLES32.GL_FALSE) {
            GLES32.glGetProgramiv(shaderProgramObject_pf, GLES32.GL_INFO_LOG_LENGTH, infoLogLength, 0);
            if (infoLogLength[0] > 0) {
                szInfoLog = GLES32.glGetProgramInfoLog(shaderProgramObject_pf);
                System.out.println("VND: shader program linking error log: " + szInfoLog);
            }
            uninitialize();
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

        int keyPressUniform;
        int materialShininessUniform;
        int materialSpecularUniform;
        int materialDiffusedUniform;
        int materialAmbientUniform;
        int lightPositionUniform;
        int lightSpecularUniform;
        int lightDiffusedUniform;
        int lightAmbientUniform;
        int projectionMatrixUniform;
        int viewMatrixUniform;
        int modelMatrixUniform;
        if (vertexShaderEnabled) {
            GLES32.glUseProgram(shaderProgramObject_pv);

            modelMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uModelMatrix");
            viewMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uViewMatrix");
            projectionMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uProjectionMatrix");
            lightAmbientUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uLightAmbient");
            lightDiffusedUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uLightDiffused");
            lightSpecularUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uLightSpecular");
            lightPositionUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uLightPositionMatrix");
            materialAmbientUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uMaterialAmbient");
            materialDiffusedUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uMaterialDiffused");
            materialSpecularUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uMaterialSpecular");
            materialShininessUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uMaterialShininess");
            keyPressUniform = GLES32.glGetUniformLocation(shaderProgramObject_pv, "uKeyPress");

        } else {
            GLES32.glUseProgram(shaderProgramObject_pf);

            modelMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uModelMatrix");
            viewMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uViewMatrix");
            projectionMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uProjectionMatrix");
            lightAmbientUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uLightAmbient");
            lightDiffusedUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uLightDiffused");
            lightSpecularUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uLightSpecular");
            lightPositionUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uLightPositionMatrix");
            materialAmbientUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uMaterialAmbient");
            materialDiffusedUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uMaterialDiffused");
            materialSpecularUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uMaterialSpecular");
            materialShininessUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uMaterialShininess");
            keyPressUniform = GLES32.glGetUniformLocation(shaderProgramObject_pf, "uKeyPress");
        }

        // sphere
        // transformations
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, -2.0f);

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
            if (whiteMaterial) {
                GLES32.glUniform3fv(materialAmbientUniform, 1, materialAmbientWhite, 0);
                GLES32.glUniform3fv(materialDiffusedUniform, 1, materialDiffusedWhite, 0);
                GLES32.glUniform3fv(materialSpecularUniform, 1, materialSpecularWhite, 0);
                float materialShininessWhite = 128.0f;
                GLES32.glUniform1f(materialShininessUniform, materialShininessWhite);
            } else {
                GLES32.glUniform3fv(materialAmbientUniform, 1, materialAmbientAlbedo, 0);
                GLES32.glUniform3fv(materialDiffusedUniform, 1, materialDiffusedAlbedo, 0);
                GLES32.glUniform3fv(materialSpecularUniform, 1, materialSpecularAlbedo, 0);
                float materialShininessAlbedo = 128.0f;
                GLES32.glUniform1f(materialShininessUniform, materialShininessAlbedo);
            }
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
        if (shaderProgramObject_pv > 0) {
            GLES32.glUseProgram(shaderProgramObject_pv);
            int[] retVal = new int[1];
            GLES32.glGetProgramiv(shaderProgramObject_pv, GLES32.GL_ATTACHED_SHADERS, retVal, 0);
            int numShaders = retVal[0];
            if (numShaders > 0) {
                int[] pShaders = new int[numShaders];
                GLES32.glGetAttachedShaders(shaderProgramObject_pv, numShaders, retVal, 0, pShaders, 0);
                for (int i = 0; i < numShaders; i++) {
                    GLES32.glDetachShader(shaderProgramObject_pv, pShaders[i]);
                    GLES32.glDeleteShader(pShaders[i]);
                    pShaders[i] = 0;
                }
            }
            GLES32.glUseProgram(0);
            GLES32.glDeleteProgram(shaderProgramObject_pv);
            shaderProgramObject_pv = 0;
        }
        if (shaderProgramObject_pf > 0) {
            GLES32.glUseProgram(shaderProgramObject_pf);
            int[] retVal = new int[1];
            GLES32.glGetProgramiv(shaderProgramObject_pf, GLES32.GL_ATTACHED_SHADERS, retVal, 0);
            int numShaders = retVal[0];
            if (numShaders > 0) {
                int[] pShaders = new int[numShaders];
                GLES32.glGetAttachedShaders(shaderProgramObject_pf, numShaders, retVal, 0, pShaders, 0);
                for (int i = 0; i < numShaders; i++) {
                    GLES32.glDetachShader(shaderProgramObject_pf, pShaders[i]);
                    GLES32.glDeleteShader(pShaders[i]);
                    pShaders[i] = 0;
                }
            }
            GLES32.glUseProgram(0);
            GLES32.glDeleteProgram(shaderProgramObject_pf);
            shaderProgramObject_pf = 0;
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

