package com.vnd.rtr5.x11_graph_paper_with_shapes;

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
    final boolean showGraphPaper = true;
    private final int[] vbo_position_triangle = new int[1];
    private final int[] vao_triangle = new int[1];
    private final int[] vbo_position_rectangle = new int[1];
    private final int[] vao_rectangle = new int[1];
    private final float[] perspectiveProjectionMatrix = new float[16];
    private final int[] vao_circle = new int[1];
    private final int[] vao_horizontal_line = new int[1];
    private final int[] vao_vertical_line = new int[1];
    private final int[] vbo_position_circle = new int[1];
    private final int[] vbo_position_horizontal_line = new int[1];
    private final int[] vbo_position_vertical_line = new int[1];
    boolean showTriangle = false;
    boolean showSquare = false;
    boolean showCircle = false;
    private GestureDetector gestureDetector = null;
    // no unsigned int in java
    private int shaderProgramObject = 0;
    private int mvpMatrixUniform;
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
        singleTap = (singleTap + 1) % 4;
        if (singleTap == 1) {
            showCircle = true;
        } else if (singleTap == 2) {
            showSquare = true;
        } else if (singleTap == 3) {
            showTriangle = true;
        } else {
            showCircle = false;
            showSquare = false;
            showTriangle = false;
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
        // GLES32.glBindAttribLocation(shaderProgramObject, VertexAttributesEnum.AMC_ATTRIBUTE_COLOR, "aColor");
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
        final float[] rectangle_position = {
                1.0f, 1.0f, 0.0f,
                -1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 0.0f,
                1.0f, -1.0f, 0.0f
        };
        float[] circle_position = new float[360 * 3];
        int index = 0;
        for (float angle = 0.0f; angle < 360.0f; angle++) {
            final float radian = angle * (float) Math.PI / 180.0f;
            final float radius = 1.0f;
            final float centerX = 0.0f, centerY = 0.0f;
            final float x = (float) Math.cos(radian) * radius + centerX;
            final float y = (float) Math.sin(radian) * radius + centerY;
            circle_position[index++] = x;
            circle_position[index++] = y;
            circle_position[index++] = 0.0f;
        }
        final float[] horizontal_line_position = {
                -3.5f, 0.0f, 0.0f,
                3.0f, 0.0f, 0.0f
        };
        final float[] vertical_line_position = {
                0.0f, -3.5f, 0.0f,
                0.0f, 3.0f, 0.0f
        };

        // triangle
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

        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, triangle_position.length * 4, floatBufferTrianglePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

        // rectangle
        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao_rectangle, 0);
        GLES32.glBindVertexArray(vao_rectangle[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position_rectangle, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position_rectangle[0]);

        // prepare rectangle vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferRectanglePosition = ByteBuffer.allocateDirect(rectangle_position.length * 4);
        byteBufferRectanglePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferRectanglePosition = byteBufferRectanglePosition.asFloatBuffer();
        floatBufferRectanglePosition.put(rectangle_position);
        floatBufferRectanglePosition.position(0);

        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, rectangle_position.length * 4, floatBufferRectanglePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

        // circle
        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao_circle, 0);
        GLES32.glBindVertexArray(vao_circle[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position_circle, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position_circle[0]);

        // prepare circle vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferCirclePosition = ByteBuffer.allocateDirect(circle_position.length * 4);
        byteBufferCirclePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferCirclePosition = byteBufferCirclePosition.asFloatBuffer();
        floatBufferCirclePosition.put(circle_position);
        floatBufferCirclePosition.position(0);

        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, circle_position.length * 4, floatBufferCirclePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

        // horizontal line
        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao_horizontal_line, 0);
        GLES32.glBindVertexArray(vao_horizontal_line[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position_horizontal_line, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position_horizontal_line[0]);

        // prepare horizontal_line vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferHorizontalLinePosition = ByteBuffer.allocateDirect(horizontal_line_position.length * 4);
        byteBufferHorizontalLinePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferHorizontalLinePosition = byteBufferHorizontalLinePosition.asFloatBuffer();
        floatBufferHorizontalLinePosition.put(horizontal_line_position);
        floatBufferHorizontalLinePosition.position(0);

        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, horizontal_line_position.length * 4, floatBufferHorizontalLinePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);

        // unbind vao
        GLES32.glBindVertexArray(0);

        // vertical line
        // vao - vertex array object
        GLES32.glGenVertexArrays(1, vao_vertical_line, 0);
        GLES32.glBindVertexArray(vao_vertical_line[0]);

        // vbo for position - vertex buffer object
        GLES32.glGenBuffers(1, vbo_position_vertical_line, 0);
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbo_position_vertical_line[0]);

        // prepare vertical_line vertices array for glBufferData
        // step1 enough bytebuffer allocate
        // step2 set byteorder
        // step3 treat as float buffer
        // step4 now fill array
        // step5 rewind to position 0
        // no sizeof() operator in java, hence given 4 below - sizeof(float)
        ByteBuffer byteBufferVerticalLinePosition = ByteBuffer.allocateDirect(vertical_line_position.length * 4);
        byteBufferVerticalLinePosition.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferVerticalLinePosition = byteBufferVerticalLinePosition.asFloatBuffer();
        floatBufferVerticalLinePosition.put(vertical_line_position);
        floatBufferVerticalLinePosition.position(0);

        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, vertical_line_position.length * 4, floatBufferVerticalLinePosition, GLES32.GL_STATIC_DRAW);
        GLES32.glVertexAttribPointer(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION, 3, GLES32.GL_FLOAT, false, 0, 0);
        GLES32.glEnableVertexAttribArray(VertexAttributesEnum.AMC_ATTRIBUTE_POSITION);
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

        float[] modelViewMatrix = new float[16];
        float[] modelViewProjectionMatrix = new float[16];

        // graph paper
        if (showGraphPaper) {
            // horizontal lines
            int lineCount = 0;
            for (float y = -10.0f; y < 10.0f; y += 0.025f) {
                // transformations
                Matrix.setIdentityM(modelViewMatrix, 0);
                Matrix.translateM(modelViewMatrix, 0, 0.0f, y, -3.5f);
                Matrix.setIdentityM(modelViewProjectionMatrix, 0);
                Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0); // order of multiplication is very important

                // push above mvp into vertex shaders mvp uniform
                GLES32.glUniformMatrix4fv(mvpMatrixUniform, 1, false, modelViewProjectionMatrix, 0);

                GLES32.glBindVertexArray(vao_horizontal_line[0]);
                GLES32.glVertexAttrib3f(VertexAttributesEnum.AMC_ATTRIBUTE_COLOR, 0.0f, 0.0f, 1.0f);
                if (lineCount % 5 == 0) {
                    GLES32.glLineWidth(2.0f);
                } else {
                    GLES32.glLineWidth(0.1f);
                }
                GLES32.glDrawArrays(GLES32.GL_LINES, 0, 2);
                GLES32.glBindVertexArray(0);

                lineCount++;
            }
            System.out.println("VND: No of horizontal lines = " + lineCount);

            // vertical lines
            lineCount = 0;
            for (float x = -10.0f; x < 10.0f; x += 0.025f) {
                // transformations
                Matrix.setIdentityM(modelViewMatrix, 0);
                Matrix.translateM(modelViewMatrix, 0, x, 0.0f, -3.5f);
                Matrix.setIdentityM(modelViewProjectionMatrix, 0);
                Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0); // order of multiplication is very important

                // push above mvp into vertex shaders mvp uniform
                GLES32.glUniformMatrix4fv(mvpMatrixUniform, 1, false, modelViewProjectionMatrix, 0);

                GLES32.glBindVertexArray(vao_vertical_line[0]);
                GLES32.glVertexAttrib3f(VertexAttributesEnum.AMC_ATTRIBUTE_COLOR, 0.0f, 0.0f, 1.0f);
                if (lineCount % 5 == 0) {
                    GLES32.glLineWidth(2.0f);
                } else {
                    GLES32.glLineWidth(0.1f);
                }
                GLES32.glDrawArrays(GLES32.GL_LINES, 0, 2);
                GLES32.glBindVertexArray(0);

                lineCount++;
            }
            System.out.println("VND: No of horizontal lines = " + lineCount);

            // X-axis
            {
                // transformations
                Matrix.setIdentityM(modelViewMatrix, 0);
                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -3.5f);
                Matrix.setIdentityM(modelViewProjectionMatrix, 0);
                Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0); // order of multiplication is very important

                // push above mvp into vertex shaders mvp uniform
                GLES32.glUniformMatrix4fv(mvpMatrixUniform, 1, false, modelViewProjectionMatrix, 0);

                GLES32.glBindVertexArray(vao_horizontal_line[0]);
                GLES32.glVertexAttrib3f(VertexAttributesEnum.AMC_ATTRIBUTE_COLOR, 1.0f, 0.0f, 0.0f);
                GLES32.glLineWidth(3.0f);
                GLES32.glDrawArrays(GLES32.GL_LINES, 0, 2);
                GLES32.glBindVertexArray(0);
            }

            // Y-axis
            {
                // transformations
                Matrix.setIdentityM(modelViewMatrix, 0);
                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -3.5f);
                Matrix.setIdentityM(modelViewProjectionMatrix, 0);
                Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0); // order of multiplication is very important

                // push above mvp into vertex shaders mvp uniform
                GLES32.glUniformMatrix4fv(mvpMatrixUniform, 1, false, modelViewProjectionMatrix, 0);

                GLES32.glBindVertexArray(vao_vertical_line[0]);
                GLES32.glVertexAttrib3f(VertexAttributesEnum.AMC_ATTRIBUTE_COLOR, 0.0f, 1.0f, 0.0f);
                GLES32.glLineWidth(3.0f);
                GLES32.glDrawArrays(GLES32.GL_LINES, 0, 2);
                GLES32.glBindVertexArray(0);
            }
        }

        // triangle
        if (showTriangle) {
            // transformations
            Matrix.setIdentityM(modelViewMatrix, 0);
            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -3.5f);
            Matrix.setIdentityM(modelViewProjectionMatrix, 0);
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0); // order of multiplication is very important

            // push above mvp into vertex shaders mvp uniform
            GLES32.glUniformMatrix4fv(mvpMatrixUniform, 1, false, modelViewProjectionMatrix, 0);

            GLES32.glBindVertexArray(vao_triangle[0]);
            GLES32.glVertexAttrib3f(VertexAttributesEnum.AMC_ATTRIBUTE_COLOR, 1.0f, 1.0f, 0.0f);
            GLES32.glLineWidth(3.0f);
            GLES32.glDrawArrays(GLES32.GL_LINE_LOOP, 0, 3);
            GLES32.glBindVertexArray(0);
        }

        // rectangle
        if (showSquare) {
            // transformations
            Matrix.setIdentityM(modelViewMatrix, 0);
            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -3.5f);
            Matrix.setIdentityM(modelViewProjectionMatrix, 0);
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0); // order of multiplication is very important

            // push above mvp into vertex shaders mvp uniform
            GLES32.glUniformMatrix4fv(mvpMatrixUniform, 1, false, modelViewProjectionMatrix, 0);
            GLES32.glVertexAttrib3f(VertexAttributesEnum.AMC_ATTRIBUTE_COLOR, 1.0f, 1.0f, 0.0f);
            GLES32.glLineWidth(3.0f);
            GLES32.glBindVertexArray(vao_rectangle[0]);
            GLES32.glDrawArrays(GLES32.GL_LINE_LOOP, 0, 4);
            GLES32.glBindVertexArray(0);
        }

        // circle
        if (showCircle) {
            // transformations
            Matrix.setIdentityM(modelViewMatrix, 0);
            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, -3.5f);
            Matrix.setIdentityM(modelViewProjectionMatrix, 0);
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, perspectiveProjectionMatrix, 0, modelViewMatrix, 0); // order of multiplication is very important

            // push above mvp into vertex shaders mvp uniform
            GLES32.glUniformMatrix4fv(mvpMatrixUniform, 1, false, modelViewProjectionMatrix, 0);
            GLES32.glVertexAttrib3f(VertexAttributesEnum.AMC_ATTRIBUTE_COLOR, 1.0f, 1.0f, 0.0f);
            GLES32.glLineWidth(3.0f);
            GLES32.glBindVertexArray(vao_circle[0]);
            GLES32.glDrawArrays(GLES32.GL_LINE_LOOP, 0, 360);
            GLES32.glBindVertexArray(0);
        }

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

        // rectangle
        if (vbo_position_rectangle[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position_rectangle, 0);
            vbo_position_rectangle[0] = 0;
        }
        if (vao_rectangle[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao_rectangle, 0);
            vao_rectangle[0] = 0;
        }

        // circle
        if (vbo_position_circle[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position_circle, 0);
            vbo_position_circle[0] = 0;
        }
        if (vao_circle[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao_circle, 0);
            vao_circle[0] = 0;
        }

        // graph paper
        if (vbo_position_horizontal_line[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position_horizontal_line, 0);
            vbo_position_horizontal_line[0] = 0;
        }
        if (vao_horizontal_line[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao_horizontal_line, 0);
            vao_horizontal_line[0] = 0;
        }
        if (vbo_position_vertical_line[0] > 0) {
            GLES32.glDeleteBuffers(1, vbo_position_vertical_line, 0);
            vbo_position_vertical_line[0] = 0;
        }
        if (vao_vertical_line[0] > 0) {
            GLES32.glDeleteVertexArrays(1, vao_vertical_line, 0);
            vao_vertical_line[0] = 0;
        }
    }
}

