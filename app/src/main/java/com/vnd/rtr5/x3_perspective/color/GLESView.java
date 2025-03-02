package com.vnd.rtr5.x3_perspective.color;

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
    private final int[] vbo_color_triangle = new int[1];
    private final int[] vao_triangle = new int[1];
    private final float[] perspectiveProjectionMatrix = new float[16];
    private GestureDetector gestureDetector = null;
    // no unsigned int in java
    private int shaderProgramObject = 0;
    private int mvpMatrixUniform;

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
                        "in vec4 aPosition;" +
                        "uniform mat4 uMVPMatrix;" +
                        "in vec4 aColor;" +
                        "out vec4 oColor;" +
                        "void main(void)" +
                        "{" +
                        "gl_Position = uMVPMatrix * aPosition;" +
                        "oColor = aColor;" +
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
                        "out vec4 fragColor;" +
                        "void main(void)" +
                        "{" +
                        "fragColor = oColor;" +
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

        final float[] triangle_position = {
                0.0f, 1.0f, 0.0f,   // apex
                -1.0f, -1.0f, 0.0f, // left bottom
                1.0f, -1.0f, 0.0f   // right bottom
        };
        final float[] triangle_color = {
                1.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 1.0f
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
        ByteBuffer byteBufferTrianglePosition = ByteBuffer.allocateDirect(triangle_position.length * 4);
        byteBufferTrianglePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferTrianglePosition = byteBufferTrianglePosition.asFloatBuffer();
        floatBufferTrianglePosition.put(triangle_position);
        floatBufferTrianglePosition.position(0);
        // now use
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, triangle_position.length * 4, floatBufferTrianglePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // vbo for color - vertex buffer object
        GLES32.glGenBuffers(1, vbo_color_triangle, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_color_triangle[0]);
        // prepare triangle vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferTriangleColor = ByteBuffer.allocateDirect(triangle_color.length * 4);
        byteBufferTriangleColor.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferTriangleColor = byteBufferTriangleColor.asFloatBuffer();
        floatBufferTriangleColor.put(triangle_color);
        floatBufferTriangleColor.position(0);
        // now use
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, triangle_color.length * 4, floatBufferTriangleColor, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_COLOR, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_COLOR);
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
        Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -5.0f);
        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelViewProjectionMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0); // order of multiplication is very important

        // push above mvp into vertex shaders mvp uniform
        GLES32.glUniformMatrix4fv(mvpMatrixUniform, 1, false, modelViewProjectionMatrix, 0);

        GLES32.glBindVertexArray(vao_triangle[0]);
        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, 3);
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
        if (vbo_color_triangle[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_color_triangle, 0);
            vbo_color_triangle[0] = 0;
        }
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

