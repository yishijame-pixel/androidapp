package com.example.funlife.ui.components.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 真 3D 骰子立方体（OpenGL ES 2.0）
 * - 6 个面，每面有正确的骰子点数
 * - 相对面之和为 7
 * - 通过 rotX/rotY/rotZ 控制旋转
 */
class DiceCubeGLView(context: Context) : GLSurfaceView(context) {
    val cubeRenderer = DiceCubeRenderer()

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        setRenderer(cubeRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

class DiceCubeRenderer : GLSurfaceView.Renderer {
    @Volatile
    var rotX = 0f
    @Volatile
    var rotY = 0f
    @Volatile
    var rotZ = 0f
    @Volatile
    var faceFront = 1  // 前面显示的点数（其他面按规则计算）

    private var program = 0
    private var posHandle = 0
    private var uvHandle = 0
    private var mvpHandle = 0
    private var samplerHandle = 0
    private var textureId = 0

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var uvBuffer: FloatBuffer
    private val uvArray = FloatArray(6 * 6 * 2)

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val tmpMatrix = FloatArray(16)

    // 立方体顶点 - 每面 6 顶点（2 三角形），共 36 顶点
    // 顺序：前 后 上 下 左 右
    private val cubeVerts = floatArrayOf(
        // 前面 (z = +0.5)
        -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f,
        -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
        // 后面 (z = -0.5)  CCW 当从 -Z 方向看
        0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f,
        0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f,
        // 上面 (y = +0.5)
        -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f,
        -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f,
        // 下面 (y = -0.5)
        -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f,
        -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f,
        // 左面 (x = -0.5)
        -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
        -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f,
        // 右面 (x = +0.5)
        0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f,
        0.5f, -0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glFrontFace(GLES20.GL_CCW)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        val vs = """
            attribute vec4 aPos;
            attribute vec2 aUV;
            uniform mat4 uMVP;
            varying vec2 vUV;
            void main() {
                gl_Position = uMVP * aPos;
                vUV = aUV;
            }
        """.trimIndent()
        val fs = """
            precision mediump float;
            varying vec2 vUV;
            uniform sampler2D uTex;
            void main() {
                gl_FragColor = texture2D(uTex, vUV);
            }
        """.trimIndent()

        program = createProgram(vs, fs)
        posHandle = GLES20.glGetAttribLocation(program, "aPos")
        uvHandle = GLES20.glGetAttribLocation(program, "aUV")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMVP")
        samplerHandle = GLES20.glGetUniformLocation(program, "uTex")

        vertexBuffer = ByteBuffer.allocateDirect(cubeVerts.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(cubeVerts); position(0)
            }
        uvBuffer = ByteBuffer.allocateDirect(uvArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        textureId = createAtlasTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
        val ratio = w.toFloat() / h.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 1f, 10f)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 每帧重建 UV（faceFront 可能变化）
        rebuildUV()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 2.5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotZ, 0f, 0f, 1f)

        Matrix.multiplyMM(tmpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tmpMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)

        GLES20.glEnableVertexAttribArray(posHandle)
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        GLES20.glEnableVertexAttribArray(uvHandle)
        uvBuffer.position(0)
        GLES20.glVertexAttribPointer(uvHandle, 2, GLES20.GL_FLOAT, false, 8, uvBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(samplerHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(uvHandle)
    }

    /** UV 重建：按当前 faceFront 计算每个面的 atlas UV */
    private fun rebuildUV() {
        val front = faceFront.coerceIn(1, 6)
        val back = 7 - front
        val faces = intArrayOf(front, back, 2, 5, 4, 3)  // 前 后 上 下 左 右
        var idx = 0
        for (v in faces) {
            // atlas: 1x6 横排，第 (v-1) 列
            val s = (v - 1) / 6f
            val e = v / 6f
            // 6 顶点，2 三角形：BL BR TR | BL TR TL
            // 对应 UV：(s,1) (e,1) (e,0) | (s,1) (e,0) (s,0)
            uvArray[idx++] = s; uvArray[idx++] = 1f
            uvArray[idx++] = e; uvArray[idx++] = 1f
            uvArray[idx++] = e; uvArray[idx++] = 0f
            uvArray[idx++] = s; uvArray[idx++] = 1f
            uvArray[idx++] = e; uvArray[idx++] = 0f
            uvArray[idx++] = s; uvArray[idx++] = 0f
        }
        uvBuffer.position(0)
        uvBuffer.put(uvArray)
        uvBuffer.position(0)
    }

    private fun createProgram(vs: String, fs: String): Int {
        val v = compileShader(GLES20.GL_VERTEX_SHADER, vs)
        val f = compileShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        return p
    }

    private fun compileShader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val status = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            android.util.Log.e("DiceGL", "Shader compile error: ${GLES20.glGetShaderInfoLog(s)}")
        }
        return s
    }

    /** 生成骰子 atlas 纹理：1x6 横排 6 个面 */
    private fun createAtlasTexture(): Int {
        val faceSize = 256
        val bm = Bitmap.createBitmap(faceSize * 6, faceSize, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = faceSize * 0.16f

        for (v in 1..6) {
            val ox = (v - 1) * faceSize.toFloat()
            // 主体白底渐变（立体感）
            paint.shader = LinearGradient(
                ox, 0f, ox, faceSize.toFloat(),
                intArrayOf(
                    0xFFFFFFFF.toInt(),
                    0xFFFAFAFA.toInt(),
                    0xFFE8E8E8.toInt()
                ),
                null,
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            c.drawRoundRect(
                RectF(ox + 4, 4f, ox + faceSize - 4, faceSize - 4f),
                r, r, paint
            )
            paint.shader = null

            // 粉色边框
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            paint.color = 0xFFFF80AB.toInt()
            c.drawRoundRect(
                RectF(ox + 4, 4f, ox + faceSize - 4, faceSize - 4f),
                r, r, paint
            )

            // 内描边
            paint.strokeWidth = 2f
            paint.color = 0x55EC407A
            c.drawRoundRect(
                RectF(ox + 12, 12f, ox + faceSize - 12, faceSize - 12f),
                r * 0.85f, r * 0.85f, paint
            )

            // 顶部高光
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(
                ox, 0f, ox, faceSize * 0.4f,
                0x80FFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP
            )
            c.drawRoundRect(
                RectF(ox + 16, 16f, ox + faceSize * 0.55f, faceSize * 0.40f),
                r * 0.6f, r * 0.6f, paint
            )
            paint.shader = null

            // 点位
            drawPips(c, paint, ox, faceSize.toFloat(), v)
        }

        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bm, 0)
        bm.recycle()
        return tex[0]
    }

    private fun drawPips(c: Canvas, p: Paint, ox: Float, size: Float, value: Int) {
        p.color = 0xFFD81B60.toInt()
        p.style = Paint.Style.FILL
        val rPip = size * 0.085f
        val cx = ox + size / 2f; val cy = size / 2f
        val left = ox + size * 0.28f; val right = ox + size * 0.72f
        val top = size * 0.28f; val bottom = size * 0.72f
        val pip = { x: Float, y: Float -> c.drawCircle(x, y, rPip, p) }
        when (value) {
            1 -> pip(cx, cy)
            2 -> { pip(left, top); pip(right, bottom) }
            3 -> { pip(left, top); pip(cx, cy); pip(right, bottom) }
            4 -> { pip(left, top); pip(right, top); pip(left, bottom); pip(right, bottom) }
            5 -> {
                pip(left, top); pip(right, top); pip(cx, cy); pip(left, bottom); pip(right, bottom)
            }
            6 -> {
                pip(left, top); pip(right, top); pip(left, cy); pip(right, cy)
                pip(left, bottom); pip(right, bottom)
            }
        }
    }
}
