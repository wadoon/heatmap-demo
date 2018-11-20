import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.image.BufferedImage
import java.util.*
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer
import javax.swing.WindowConstants

/**
 *
 * @author Alexander Weigl
 * @version 1 (20.11.18)
 */
object HeatmapFrame : JFrame("Heatmap Demo"), ComponentListener {
    override fun componentMoved(e: ComponentEvent?) {}

    override fun componentHidden(e: ComponentEvent?) {}

    override fun componentShown(e: ComponentEvent?) {}

    override fun componentResized(e: ComponentEvent?) {
        heatmap.redraw(size)
    }

    val heatmap = Heatmap()

    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        val myTimer = Timer(10, this::captureMousePosition)
        myTimer.isRepeats = true
        myTimer.start()
        addComponentListener(this)
        layout = BorderLayout()
        add(ImagePanel)
    }

    fun updateHeatMap(minfo: PointerInfo) {
        val withinWindow =
            minfo.location.x >= locationOnScreen.x &&
                    minfo.location.y >= locationOnScreen.y &&
                    minfo.location.x <= (locationOnScreen.x + size.width) &&
                    minfo.location.y <= (locationOnScreen.y + size.height)
        if (withinWindow) {
            val x = (minfo.location.x - locationOnScreen.x).toFloat() / size.width
            val y = (minfo.location.y - locationOnScreen.y).toFloat() / size.height
            heatmap.announce(x, y)
        }
    }

    object ImagePanel : JPanel(true) {
        override fun paintChildren(g: Graphics) {
            if (size.width != heatmap.image.width || size.height != heatmap.image.height)
                heatmap.redraw(size)
            g.drawImage(heatmap.image, 0, 0, null)
        }
    }

    var cnt = 0
    fun captureMousePosition(event: ActionEvent) {
        val info = MouseInfo.getPointerInfo()
        updateHeatMap(info)
        if (cnt++ % 10 == 0) {
            invalidate()
            repaint()
            repaint()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        HeatmapFrame.size = Dimension(600, 400)
        HeatmapFrame.isVisible = true
    }
}

data class MousePosition(val x: Float, val y: Float)

class Heatmap(var size: Dimension = Dimension(1, 1)) {
    val points = ArrayList<MousePosition>()
    //var image = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB)
    val coneHeight = 25
    val coneWidth = 25

    var cone = createCone(coneWidth, coneHeight);
    var raster = IntArray(size.width * size.height) { 0 }

    val image: BufferedImage
        get() {
            val image = BufferedImage(size.width, size.height, BufferedImage.TYPE_BYTE_GRAY)
            var min = raster.min()!!
            var max = raster.max()!!
            if (max == 0) return image

            val color = FloatArray(3) { 0f }
            //val minColor = Color.GREEN.getRGBColorComponents(null)
            //val maxColor = Color.RED.getRGBColorComponents(null)

            for (x in 0 until size.width) {
                for (y in 0 until size.height) {
                    val value = raster[posInArray(x, y)]
                    val interpolant = (value - min).toFloat() / max.toFloat()
                    val c = 255 * interpolant
                    image.raster.setSample(x, y, 0, c.toInt())
                }
            }
            return image
        }


    fun announce(x: Float, y: Float) {
        val m = MousePosition(x, y)
        points.add(m)
        draw(x, y)
    }

    private fun draw(x: Float, y: Float) {
        val xOnCanvas = x * size.width
        val yOnCanvas = y * size.height

        for (x in 0 until coneWidth) {
            for (y in 0 until coneHeight) {
                val xx = (xOnCanvas + (x - coneWidth / 2)).toInt()
                val yy = (yOnCanvas + (y - coneWidth / 2)).toInt()
                if (xx >= 0 && yy >= 0 && xx < size.width && yy < size.height) {
                    raster[posInArray(xx, yy)] += cone[x + y * coneWidth]
                }
            }
        }
    }

    private fun posInArray(xx: Int, yy: Int) = xx + yy * size.width

    fun redraw(newSize: Dimension = size) {
        println("newSize = [${newSize}]")
        size = newSize
        raster = IntArray(size.width * size.height) { 0 }
        points.forEach { draw(it.x, it.y) }
    }
}

data class ColorFixpoint(val minValue: Float, val r: Int, val g: Int, val b: Int)
class ColorMap(
    val fixpoints: Array<ColorFixpoint>
) {
    init {
        Arrays.sort(fixpoints) { o1, o2 ->
            (o2.minValue - o1.minValue).toInt()
        }
    }

    fun getColor(x: Float): Unit {
        //Arrays.binarySearch(fixpoints)
    }
}

private fun createCone(width: Int, height: Int, min: Int = 0, max: Int = 1000): IntArray {
    val centerX = width / 2
    val centerY = width / 2

    //Maybe replace by Manhatten distance
    fun distFromCenter(x: Int, y: Int) = Math.hypot((centerX - x).toDouble(), (centerY - y).toDouble())

    val maxDistance = distFromCenter(0, 0)

    fun value(x: Int, y: Int) =
        min + ((maxDistance - distFromCenter(x, y)) / maxDistance * max).toInt()

    val raster = IntArray(width * height) { 0 }
    for (x in 0 until width)
        for (y in 0 until height)
            raster[x + y * width] = value(x, y)

    for (x in 0 until width) {
        for (y in 0 until height) {
            System.out.format(" %8d", raster[x + y * width])
        }
        println()
    }

    return raster
}