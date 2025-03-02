package com.vnd.rtr5.x16_interleave;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.rtr5.R;
import com.vnd.rtr5.common.VertexAttributesEnum;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLESView extends GLSurfaceView
        implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final float[] lightAmbient = {0.1f, 0.1f, 0.1f, 1.0f}; // ambient light
    private final float[] lightDiffused = {1.0f, 1.0f, 1.0f, 1.0f}; // white diffused light
    private final float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f}; // white specular light
    private final float[] lightPosition = {100.0f, 100.0f, 100.0f, 1.0f};
    private final float[] materialAmbient = {1.0f, 1.0f, 1.0f, 1.0f};
    private final float[] materialDiffused = {1.0f, 1.0f, 1.0f, 1.0f};
    private final float[] materialSpecular = {1.0f, 1.0f, 1.0f, 1.0f};
    private final int[] vbo_cube = new int[1];
    private final int[] vao_cube = new int[1];
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] texture_marble = new int[1];
    private final Context context;
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
    private float cAngle = 0.0f;
    private int textureSamplerUniform;

    public GLESView(Context _context) {
        super(_context);
        context = _context;

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
        bLightingEnabled = !bLightingEnabled;
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
                        "in vec4 aColor;" +
                        "in vec3 aNormal;" +
                        "in vec2 aTextureCoordinates;" +
                        "uniform int uKeyPress;" +
                        "uniform mat4 uModelMatrix;" +
                        "uniform mat4 uViewMatrix;" +
                        "uniform mat4 uProjectionMatrix;" +
                        "uniform vec4 uLightPositionMatrix;" +
                        "out vec3 oLightDirection;" +
                        "out vec3 oViewerVector;" +
                        "out vec4 oColor;" +
                        "out vec3 oTransformedNormals;" +
                        "out vec2 oTextureCoordinates;" +
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
                        "oColor = aColor;" +
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
                        "in vec4 oColor;" +
                        "in vec2 oTextureCoordinates;" +
                        "uniform sampler2D uTextureSampler;" +
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
                        "vec4 tex = texture(uTextureSampler, oTextureCoordinates);" +
                        "if (uKeyPress == 1)" +
                        "{" +
                        "vec3 normalizedTransformedNormals = normalize(oTransformedNormals);" +
                        "vec3 normalizedLightDirection = normalize(oLightDirection);" +
                        "vec3 normalizedViewerVector = normalize(oViewerVector);" +
                        "vec3 ambientLight = uLightAmbient * uMaterialAmbient;" +
                        "vec3 diffusedLight = uLightDiffused * uMaterialDiffused * max(dot(normalizedLightDirection, normalizedTransformedNormals), 0.0f);"
                        +
                        "vec3 reflectionVector = reflect(-normalizedLightDirection, normalizedTransformedNormals);" +
                        "vec3 lightSpecular = uLightSpecular * uMaterialSpecular * pow(max(dot(reflectionVector, normalizedViewerVector), 0.0f), uMaterialShininess);"
                        +
                        "vec3 phong_ADS_Light = ambientLight + diffusedLight + lightSpecular;" +
                        "fragColor = tex * oColor * vec4(phong_ADS_Light, 1.0f);" +
                        "}" +
                        "else" +
                        "{" +
                        "vec3 phong_ADS_Light = vec3(1.0f, 1.0f, 1.0f);" +
                        "fragColor = tex * oColor * vec4(phong_ADS_Light, 1.0f);" +
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
        GLES32.glBindAttribLocation(shaderProgramObject, VertexAttributesEnum.AMC_ATTRIBUTE_COLOR, "aColor");
        GLES32.glBindAttribLocation(shaderProgramObject, VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL, "aNormal");
        GLES32.glBindAttribLocation(shaderProgramObject, VertexAttributesEnum.AMC_ATTRIBUTE_TEXTURE_COORDINATES, "aTextureCoordinates");
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
        textureSamplerUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uTextureSampler");

        final float[] cube_pcnt = {
                // position // color // normal //texture_coordinates
                // top
                1.0f, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f,
                -1.0f, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f,
                -1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,

                // bottom
                1.0f, -1.0f, -1.0f, 1.0f, 0.5f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f,
                -1.0f, -1.0f, -1.0f, 1.0f, 0.5f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 1.0f, 1.0f, 0.5f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
                1.0f, -1.0f, 1.0f, 1.0f, 0.5f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 1.0f,

                // front
                1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                -1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
                1.0f, -1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,

                // back
                1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
                -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
                1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f,

                // right
                1.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                1.0f, -1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                1.0f, -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f,

                // left
                -1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                -1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, -1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                -1.0f, -1.0f, 1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f};

        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao_cube, 0);
        GLES32.glBindVertexArray(vao_cube[0]);

        // vbo - vertex buffer object
        GLES32.glGenBuffers(1, vbo_cube, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_cube[0]);
        // prepare cube vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferCubePosition = ByteBuffer.allocateDirect(cube_pcnt.length * 4);
        byteBufferCubePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferCubePosition = byteBufferCubePosition.asFloatBuffer();
        floatBufferCubePosition.put(cube_pcnt);
        floatBufferCubePosition.position(0);
        // now use
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, cube_pcnt.length * 4, floatBufferCubePosition,
                GLES32.GL_STATIC_DRAW);
        // position
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, 3, GLES32.GL_FLOAT, false, 11 * 4,
                0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION);
        // color
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_COLOR, 3, GLES32.GL_FLOAT, false, 11 * 4,
                3 * 4);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_COLOR);
        // normal
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL, 3, GLES32.GL_FLOAT, false, 11 * 4,
                6 * 4);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL);
        // texture_coordinates
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_TEXTURE_COORDINATES, 2, GLES32.GL_FLOAT, false, 11 * 4,
                9 * 4);
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

        // loading images to create texture
        texture_marble[0] = loadGLTexture(R.raw.marble);

        // Tell OpenGL to enable texture
        GLES32.glEnable(GLES32.GL_TEXTURE_2D);

        // initialize perspectiveProjectionMatrix
        Matrix.setIdentityM(perspectiveProjectionMatrix, 0);
    }

    private int loadGLTexture(int imageResourceID) {
        // create bitmap factory options object
        BitmapFactory.Options options = new BitmapFactory.Options();

        // don't scale the image
        options.inScaled = false;

        // create the bitmap image from image resource
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imageResourceID, options);

        // create image texture
        int[] texture = new int[1];
        GLES32.glGenTextures(1, texture, 0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[0]);
        GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT, 1);

        // set texture parameters
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR_MIPMAP_LINEAR);

        // create multiple MIPMAP images
        GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES32.glGenerateMipmap(GLES32.GL_TEXTURE_2D);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

        return texture[0];
    }

    private void printGLInfo(GL10 gl) {
        // code
        System.out.println("VND: OpenGL ES Vendor: " + gl.glGetString(GL10.GL_VENDOR));
        System.out.println("VND: OpenGL ES Renderer: " + gl.glGetString(GL10.GL_RENDERER));
        System.out.println("VND: OpenGL ES Version: " + gl.glGetString(GL10.GL_VERSION));
        System.out.println(
                "VND: OpenGL ES Shading Language Version: " + gl.glGetString(GLES32.GL_SHADING_LANGUAGE_VERSION));

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

        // cube
        // transformations
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM(modelMatrix, 0, cAngle, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(modelMatrix, 0, cAngle, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(modelMatrix, 0, cAngle, 0.0f, 0.0f, 1.0f);

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
            GLES32.glUniform3fv(materialAmbientUniform, 1, materialAmbient, 0);
            GLES32.glUniform3fv(materialDiffusedUniform, 1, materialDiffused, 0);
            GLES32.glUniform3fv(materialSpecularUniform, 1, materialSpecular, 0);
            float materialShininess = 128.0f;
            GLES32.glUniform1f(materialShininessUniform, materialShininess);
        } else {
            GLES32.glUniform1i(keyPressUniform, 0);
        }

        // bind texture
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture_marble[0]);
        GLES32.glUniform1i(textureSamplerUniform, 0);

        // bind vao
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

    private void update() {
        // code
        cAngle -= 1.0f;
        if (cAngle <= 0.0f) {
            cAngle += 360.0f;
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

        // cube
        if (vbo_cube[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_cube, 0);
            vbo_cube[0] = 0;
        }
        if (vao_cube[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao_cube, 0);
            vao_cube[0] = 0;
        }
    }
}
