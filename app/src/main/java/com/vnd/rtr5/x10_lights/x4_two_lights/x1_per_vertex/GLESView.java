package com.vnd.rtr5.x10_lights.x4_two_lights.x1_per_vertex;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.vnd.rtr5.common.Light;
import com.vnd.rtr5.common.VertexAttributesEnum;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLESView extends GLSurfaceView implements GLSurfaceView.Renderer, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    final float[] materialAmbient = {0.0f, 0.0f, 0.0f, 1.0f};
    final float[] materialDiffused = {1.0f, 1.0f, 1.0f, 1.0f};
    final float[] materialSpecular = {1.0f, 1.0f, 1.0f, 1.0f};
    private final int[] vbo_position_pyramid = new int[1];
    private final int[] vbo_normal_pyramid = new int[1];
    private final int[] vao_pyramid = new int[1];
    private final int[] lightDiffusedUniform = new int[2];
    private final int[] lightPositionUniform = new int[2];
    private final int[] lightAmbientUniform = new int[2];
    private final int[] lightSpecularUniform = new int[2];
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final Light[] light = new Light[2];
    boolean bLightingEnabled = true;
    boolean bAnimationEnabled = true;
    float pAngle = 0.0f;
    private GestureDetector gestureDetector = null;
    // no unsigned int in java
    private int shaderProgramObject = 0;
    private int modelMatrixUniform = 0;
    private int viewMatrixUniform = 0;
    private int projectionMatrixUniform = 0;
    private int materialAmbientUniform = 0;
    private int materialDiffusedUniform = 0;
    private int materialSpecularUniform = 0;
    private int materialShininessUniform = 0;
    private int keyPressUniform = 0;
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
        if (bAnimationEnabled) {
            update();
        }
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
        singleTap = (singleTap + 1) % 4;
        if (singleTap == 1) {
            bLightingEnabled = true;
            bAnimationEnabled = false;
        } else if (singleTap == 2) {
            bLightingEnabled = false;
            bAnimationEnabled = false;
        } else if (singleTap == 3) {
            bLightingEnabled = false;
            bAnimationEnabled = true;
        } else {
            bLightingEnabled = true;
            bAnimationEnabled = true;
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
        final String vertexShaderSourceCode =
                "#version 320 es" +
                        "\n" +
                        "precision mediump int;" +
                        "in vec4 aPosition;" +
                        "in vec3 aNormal;" +
                        "uniform vec3 uLightAmbient[2];" +
                        "uniform vec3 uLightDiffused[2];" +
                        "uniform vec3 uLightSpecular[2];" +
                        "uniform vec4 uLightPositionMatrix[2];" +
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
                        "oPhong_ADS_Light = vec3(0.0f, 0.0f, 0.0f);" +
                        "if (uKeyPress == 1)" +
                        "{" +
                        "vec3 lightDirection[2];" +
                        "vec3 reflectionVector[2];" +
                        "vec3 ambientLight[2];" +
                        "vec3 diffusedLight[2];" +
                        "vec3 lightSpecular[2];" +
                        "vec4 eyeCoordinates = uViewMatrix * uModelMatrix * aPosition;" +
                        "vec3 transformedNormals = normalize(mat3(uViewMatrix * uModelMatrix) * aNormal);" +
                        "vec3 viewerVector = normalize(-eyeCoordinates.xyz);" +
                        "for (int i = 0; i < 2; i++) " +
                        "{" +
                        "lightDirection[i] = normalize(vec3(uLightPositionMatrix[i] - eyeCoordinates));" +
                        "reflectionVector[i] = reflect(-lightDirection[i], transformedNormals);" +
                        "ambientLight[i] = uLightAmbient[i] * uMaterialAmbient;" +
                        "diffusedLight[i] = uLightDiffused[i] * uMaterialDiffused * max(dot(lightDirection[i], transformedNormals), 0.0f);" +
                        "lightSpecular[i] = uLightSpecular[i] * uMaterialSpecular * pow(max(dot(reflectionVector[i], viewerVector), 0.0f), uMaterialShininess);" +
                        "oPhong_ADS_Light = oPhong_ADS_Light + ambientLight[i] + diffusedLight[i] + lightSpecular[i];" +
                        "}" +
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

        // get shader uniform locations - must be after linkage
        modelMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uModelMatrix");
        viewMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uViewMatrix");
        projectionMatrixUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uProjectionMatrix");
        lightAmbientUniform[0] = GLES32.glGetUniformLocation(shaderProgramObject, "uLightAmbient[0]");
        lightDiffusedUniform[0] = GLES32.glGetUniformLocation(shaderProgramObject, "uLightDiffused[0]");
        lightSpecularUniform[0] = GLES32.glGetUniformLocation(shaderProgramObject, "uLightSpecular[0]");
        lightPositionUniform[0] = GLES32.glGetUniformLocation(shaderProgramObject, "uLightPositionMatrix[0]");
        lightAmbientUniform[1] = GLES32.glGetUniformLocation(shaderProgramObject, "uLightAmbient[1]");
        lightDiffusedUniform[1] = GLES32.glGetUniformLocation(shaderProgramObject, "uLightDiffused[1]");
        lightSpecularUniform[1] = GLES32.glGetUniformLocation(shaderProgramObject, "uLightSpecular[1]");
        lightPositionUniform[1] = GLES32.glGetUniformLocation(shaderProgramObject, "uLightPositionMatrix[1]");
        materialAmbientUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uMaterialAmbient");
        materialDiffusedUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uMaterialDiffused");
        materialSpecularUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uMaterialSpecular");
        materialShininessUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uMaterialShininess");
        keyPressUniform = GLES32.glGetUniformLocation(shaderProgramObject, "uKeyPress");

        final float[] pyramid_position = {
                // front
                0.0f, 1.0f, 0.0f, // front-top
                -1.0f, -1.0f, 1.0f, // front-left
                1.0f, -1.0f, 1.0f, // front-right

                // right
                0.0f, 1.0f, 0.0f, // right-top
                1.0f, -1.0f, 1.0f, // right-left
                1.0f, -1.0f, -1.0f, // right-right

                // back
                0.0f, 1.0f, 0.0f, // back-top
                1.0f, -1.0f, -1.0f, // back-left
                -1.0f, -1.0f, -1.0f, // back-right

                // left
                0.0f, 1.0f, 0.0f, // left-top
                -1.0f, -1.0f, -1.0f, // left-left
                -1.0f, -1.0f, 1.0f, // left-right
        };
        final float[] pyramid_normal = {
                // front
                0.000000f, 0.447214f, 0.894427f, // front-top
                0.000000f, 0.447214f, 0.894427f, // front-left
                0.000000f, 0.447214f, 0.894427f, // front-right

                // right
                0.894427f, 0.447214f, 0.000000f, // right-top
                0.894427f, 0.447214f, 0.000000f, // right-left
                0.894427f, 0.447214f, 0.000000f, // right-right

                // back
                0.000000f, 0.447214f, -0.894427f, // back-top
                0.000000f, 0.447214f, -0.894427f, // back-left
                0.000000f, 0.447214f, -0.894427f, // back-right

                // left
                -0.894427f, 0.447214f, 0.000000f, // left-top
                -0.894427f, 0.447214f, 0.000000f, // left-left
                -0.894427f, 0.447214f, 0.000000f, // left-right
        };

        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao_pyramid, 0);
        GLES32.glBindVertexArray(vao_pyramid[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position_pyramid, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position_pyramid[0]);
        // prepare pyramid vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferPyramidPosition = ByteBuffer.allocateDirect(pyramid_position.length * 4);
        byteBufferPyramidPosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferPyramidPosition = byteBufferPyramidPosition.asFloatBuffer();
        floatBufferPyramidPosition.put(pyramid_position);
        floatBufferPyramidPosition.position(0);
        // now use
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, pyramid_position.length * 4, floatBufferPyramidPosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // vbo for normal - vertex buffer object
        GLES32.glGenBuffers(1, vbo_normal_pyramid, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_normal_pyramid[0]);
        // prepare pyramid vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferPyramidNormal = ByteBuffer.allocateDirect(pyramid_normal.length * 4);
        byteBufferPyramidNormal.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferPyramidNormal = byteBufferPyramidNormal.asFloatBuffer();
        floatBufferPyramidNormal.put(pyramid_normal);
        floatBufferPyramidNormal.position(0);
        // now use
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, pyramid_normal.length * 4, floatBufferPyramidNormal, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_NORMAL);
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

        // initialize perspectiveProjectionMatrix
        Matrix.setIdentityM(perspectiveProjectionMatrix, 0);

        light[0] = new Light();
        light[1] = new Light();
        light[0].ambient[0] = 0.0f;
        light[0].ambient[1] = 0.0f;
        light[0].ambient[2] = 0.0f;
        light[1].ambient[0] = 0.0f;
        light[1].ambient[1] = 0.0f;
        light[1].ambient[2] = 0.0f;
        light[0].diffused[0] = 1.0f;
        light[0].diffused[1] = 0.0f;
        light[0].diffused[2] = 0.0f;
        light[1].diffused[0] = 0.0f;
        light[1].diffused[1] = 0.0f;
        light[1].diffused[2] = 1.0f;
        light[0].specular[0] = 1.0f;
        light[0].specular[1] = 0.0f;
        light[0].specular[2] = 0.0f;
        light[1].specular[0] = 0.0f;
        light[1].specular[1] = 0.0f;
        light[1].specular[2] = 1.0f;
        light[0].position[0] = -2.0f;
        light[0].position[1] = 0.0f;
        light[0].position[2] = 0.0f;
        light[0].position[3] = 1.0f;
        light[1].position[0] = 2.0f;
        light[1].position[1] = 0.0f;
        light[1].position[2] = 0.0f;
        light[1].position[3] = 1.0f;
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
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.rotateM(modelMatrix, 0, pAngle, -1.0f, 0.0f, 0.0f);
        Matrix.rotateM(modelMatrix, 0, pAngle, 0.0f, -1.0f, 0.0f);
        Matrix.rotateM(modelMatrix, 0, pAngle, 0.0f, 0.0f, -1.0f);

        float[] viewMatrix = new float[16];
        Matrix.setIdentityM(viewMatrix, 0);

        // push above mvp into vertex shaders mvp uniform
        GLES32.glUniformMatrix4fv(modelMatrixUniform, 1, false, modelMatrix, 0);
        GLES32.glUniformMatrix4fv(viewMatrixUniform, 1, false, viewMatrix, 0);
        GLES32.glUniformMatrix4fv(projectionMatrixUniform, 1, false, perspectiveProjectionMatrix, 0);

        if (bLightingEnabled) {
            GLES32.glUniform1i(keyPressUniform, 1);
            GLES32.glUniform3fv(lightAmbientUniform[0], 1, light[0].ambient, 0);
            GLES32.glUniform3fv(lightDiffusedUniform[0], 1, light[0].diffused, 0);
            GLES32.glUniform3fv(lightSpecularUniform[0], 1, light[0].specular, 0);
            GLES32.glUniform4fv(lightPositionUniform[0], 1, light[0].position, 0);
            GLES32.glUniform3fv(lightAmbientUniform[1], 1, light[1].ambient, 0);
            GLES32.glUniform3fv(lightDiffusedUniform[1], 1, light[1].diffused, 0);
            GLES32.glUniform3fv(lightSpecularUniform[1], 1, light[1].specular, 0);
            GLES32.glUniform4fv(lightPositionUniform[1], 1, light[1].position, 0);
            GLES32.glUniform3fv(materialAmbientUniform, 1, materialAmbient, 0);
            GLES32.glUniform3fv(materialDiffusedUniform, 1, materialDiffused, 0);
            GLES32.glUniform3fv(materialSpecularUniform, 1, materialSpecular, 0);
            float materialShininess = 128.0f;
            GLES32.glUniform1f(materialShininessUniform, materialShininess);
        } else {
            GLES32.glUniform1i(keyPressUniform, 0);
        }

        GLES32.glBindVertexArray(vao_pyramid[0]);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, 12);
        GLES32.glBindVertexArray(0);
        GLES32.glUseProgram(0);

        // render
        requestRender();
    }

    private void update() {
        pAngle += 1.0f;
        if (pAngle >= 360) {
            pAngle -= 360.0f;
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

        // pyramid
        if (vbo_normal_pyramid[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_normal_pyramid, 0);
            vbo_normal_pyramid[0] = 0;
        }
        if (vbo_position_pyramid[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position_pyramid, 0);
            vbo_position_pyramid[0] = 0;
        }
        if (vao_pyramid[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao_pyramid, 0);
            vao_pyramid[0] = 0;
        }
    }
}

