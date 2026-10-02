package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xff0b1017)
private val Panel = Color(0xff111923)
private val Panel2 = Color(0xff17222e)
private val Line = Color(0xff263644)
private val Cyan = Color(0xff35d7ef)
private val Muted = Color(0xff8c9cac)

@Composable
fun StudioProScreen() {
    var workspace by remember { mutableStateOf("Modeling") }
    var mode by remember { mutableStateOf("Edit") }
    var shading by remember { mutableStateOf("Material") }
    var frame by remember { mutableIntStateOf(42) }
    var selected by remember { mutableStateOf("Body") }
    var showGrid by remember { mutableStateOf(true) }
    var showGizmo by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Ink)) {
        StudioTopBar(workspace, { workspace = it })
        Row(Modifier.weight(1f).fillMaxWidth()) {
            ToolRail(mode, { mode = it })
            Column(Modifier.weight(1f)) {
                ViewportHeader(shading, { shading = it }, showGrid, { showGrid = !showGrid }, showGizmo, { showGizmo = !showGizmo })
                Row(Modifier.weight(1f)) {
                    Viewport(shading, showGrid, showGizmo, selected)
                    Inspector(selected, { selected = it })
                }
            }
        }
        Timeline(frame, { frame = it }, playing, { playing = !playing })
    }
}

@Composable private fun StudioTopBar(active: String, onWorkspace: (String) -> Unit) {
    Column(Modifier.background(Color(0xff0e151e))) {
        Row(Modifier.height(48.dp).fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(27.dp).background(Cyan, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) { Text("3D", color = Ink, fontWeight = FontWeight.Black, fontSize = 10.sp) }
            Spacer(Modifier.width(9.dp)); Text("3D Studio", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(" PRO MAX", color = Cyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(15.dp)); Text("/", color = Line); Spacer(Modifier.width(10.dp)); Text("Project Nebula", color = Color.White, fontSize = 12.sp); Text("  •  scene_04.blend", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            TopAction(Icons.Default.Save, "Save"); TopAction(Icons.Default.Undo, "Undo"); TopAction(Icons.Default.Redo, "Redo"); TopAction(Icons.Default.FileOpen, "Import"); TopAction(Icons.Default.Upload, "Export"); TopAction(Icons.Default.Settings, "Settings")
            Spacer(Modifier.width(8.dp)); Box(Modifier.size(28.dp).background(Color(0xff203343), RoundedCornerShape(50)), contentAlignment = Alignment.Center) { Text("A", color = Cyan, fontWeight = FontWeight.Bold) }
        }
        Row(Modifier.fillMaxWidth().height(35.dp).horizontalScroll(rememberScrollState()).padding(start = 12.dp), verticalAlignment = Alignment.Bottom) {
            listOf("Modeling","Sculpt","UV","Texture","Material","Rigging","Animation","VFX","Render","Video","AR/VR","Cloud").forEach { name ->
                Text(name, color = if (name == active) Color.White else Muted, fontSize = 11.sp, fontWeight = if (name == active) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp).clickable { onWorkspace(name) })
            }
        }
    }
}

@Composable private fun TopAction(icon: ImageVector, label: String) { Row(Modifier.padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, label, tint = Muted, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text(label, color = Muted, fontSize = 10.sp) } }

@Composable private fun ToolRail(mode: String, onMode: (String) -> Unit) {
    val tools = listOf(Icons.Default.TouchApp to "Select", Icons.Default.OpenWith to "Move", Icons.Default.RotateRight to "Rotate", Icons.Default.OpenInFull to "Scale", Icons.Default.AddBox to "Extrude", Icons.Default.CropSquare to "Inset", Icons.Default.Build to "Bevel", Icons.Default.ContentCut to "Loop Cut", Icons.Default.GpsFixed to "Knife", Icons.Default.CallMerge to "Boolean", Icons.Default.Flip to "Mirror", Icons.Default.GridOn to "Subdivide", Icons.Default.Link to "Weld", Icons.Default.JoinFull to "Bridge", Icons.Default.FormatColorFill to "Fill", Icons.Default.SwapCalls to "Spin")
    Column(Modifier.width(64.dp).fillMaxHeight().background(Color(0xff0d151e)).padding(vertical = 7.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        tools.forEachIndexed { i, (icon, label) ->
            ToolButton(icon, label, i == 0, onMode)
        }
        Spacer(Modifier.height(6.dp)); Divider(color = Line, modifier = Modifier.width(42.dp)); Spacer(Modifier.height(6.dp))
        ToolButton(Icons.Default.WifiTethering, "Snap", false, onMode); ToolButton(Icons.Default.Brush, "Sculpt", mode == "Sculpt", onMode)
    }
}

@Composable private fun ToolButton(icon: ImageVector, label: String, active: Boolean, onMode: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = 2.dp).size(52.dp)
            .background(if (active) Color(0xff16495a) else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable { onMode(if (label == "Sculpt") "Sculpt" else "Edit") },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, tint = if (active) Cyan else Muted, modifier = Modifier.size(19.dp))
        Text(label, color = if (active) Color.White else Muted, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable private fun ViewportHeader(shading: String, onShading: (String) -> Unit, grid: Boolean, onGrid: () -> Unit, gizmo: Boolean, onGizmo: () -> Unit) {
    Row(Modifier.height(34.dp).fillMaxWidth().background(Color(0xff121c27)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Perspective", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold); Icon(Icons.Default.KeyboardArrowDown, null, tint = Muted, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(15.dp)); Text("User Perspective", color = Muted, fontSize = 10.sp)
        Spacer(Modifier.weight(1f)); listOf("Wireframe","Solid","Material","Rendered").forEach { name -> MiniToggle(name, shading == name, { onShading(name) }) }; Spacer(Modifier.width(10.dp)); MiniToggle("Grid", grid, onGrid); MiniToggle("X-Ray", false, {}); MiniToggle("Gizmo", gizmo, onGizmo); Icon(Icons.Default.CameraAlt, "Camera View", tint = Muted, modifier = Modifier.padding(horizontal = 6.dp).size(15.dp)); Icon(Icons.Default.LightMode, "Lighting", tint = Muted, modifier = Modifier.padding(horizontal = 6.dp).size(15.dp))
    }
}
@Composable private fun MiniToggle(text: String, active: Boolean, onClick: () -> Unit) { Text(text, color = if (active) Cyan else Muted, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp).clickable { onClick() }) }

@Composable private fun Viewport(shading: String, grid: Boolean, gizmo: Boolean, selected: String) {
    Box(Modifier.fillMaxSize().background(Color(0xff151e27))) {
        Canvas(Modifier.fillMaxSize()) { drawSciFiMotorcycle(grid, shading) }
        if (gizmo) {
            Column(Modifier.align(Alignment.TopStart).padding(20.dp).background(Color(0xbb111923), RoundedCornerShape(5.dp)).border(1.dp, Color(0xff344b5e), RoundedCornerShape(5.dp)).padding(7.dp)) { Text("${selected.uppercase()}  •  ACTIVE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("Transform Gizmo", color = Color.White, fontSize = 10.sp); Text("W  0.00 m     E  0.00°", color = Muted, fontSize = 9.sp) }
            Text("X", color = Color(0xfff05858), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).offset(x = 118.dp, y = (-75).dp))
            Text("Y", color = Color(0xff66da91), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).offset(x = (-10).dp, y = (-148).dp))
            Text("Z", color = Color(0xff4d9dff), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).offset(x = (-75).dp, y = (-14).dp))
        }
        Row(Modifier.align(Alignment.BottomStart).padding(12.dp).background(Color(0xcc111923), RoundedCornerShape(5.dp)).padding(6.dp), verticalAlignment = Alignment.CenterVertically) { Text("Edit Mode", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(10.dp)); Text("Vertex", color = Color.White, fontSize = 9.sp); Text("  Edge", color = Muted, fontSize = 9.sp); Text("  Face", color = Muted, fontSize = 9.sp) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSciFiMotorcycle(grid: Boolean, shading: String) {
    val w = size.width; val h = size.height; val horizon = h * .64f
    if (grid) { for (i in 0..18) { val x = w * i / 18f; drawLine(Color(0xff263643), Offset(x, horizon), Offset(w / 2 + (x - w / 2) * .35f, h), 1f) }; for (i in 0..8) { val y = horizon + (h - horizon) * i / 8f; drawLine(Color(0xff253541), Offset(0f,y), Offset(w,y), 1f) } }
    drawOval(Color(0xff071016), Offset(w*.20f,h*.55f), Size(w*.17f,h*.30f)); drawOval(Color(0xff071016), Offset(w*.64f,h*.50f), Size(w*.17f,h*.30f)); drawOval(Color(0xff26323a), Offset(w*.205f,h*.565f), Size(w*.14f,h*.27f), style=Stroke(4f)); drawOval(Color(0xff26323a), Offset(w*.645f,h*.515f), Size(w*.14f,h*.27f), style=Stroke(4f))
    val body = Path().apply { moveTo(w*.28f,h*.56f); cubicTo(w*.33f,h*.40f,w*.42f,h*.35f,w*.54f,h*.41f); lineTo(w*.68f,h*.50f); lineTo(w*.60f,h*.61f); lineTo(w*.40f,h*.62f); close() }
    drawPath(body, Brush.linearGradient(listOf(Color(0xff1b3545), Color(0xff4e9cac), Color(0xff142632))), style=Fill); drawPath(body, Color(0xff77d9e7), style=Stroke(2f))
    drawLine(Color(0xff0e161d), Offset(w*.37f,h*.53f), Offset(w*.52f,h*.43f), 7f); drawLine(Color(0xff9be7ee), Offset(w*.38f,h*.51f), Offset(w*.52f,h*.42f), 1.5f); drawLine(Color(0xffa4dbe1), Offset(w*.50f,h*.42f), Offset(w*.68f,h*.50f), 3f); drawLine(Color(0xff8598a0), Offset(w*.51f,h*.43f), Offset(w*.65f,h*.25f), 4f)
    drawCircle(Color(0xffd8fbff), Offset(w*.66f,h*.43f), w*.022f); drawCircle(Color(0xff7ee9ff), Offset(w*.66f,h*.43f), w*.035f, style=Stroke(2f)); drawCircle(Color(0xff172027), Offset(w*.455f,h*.54f), w*.07f); drawCircle(Color(0xffbc6c36), Offset(w*.455f,h*.54f), w*.038f); drawLine(Color(0xffe0a35a), Offset(w*.45f,h*.48f), Offset(w*.50f,h*.60f), 4f); drawLine(Color(0xffa4b5bb), Offset(w*.47f,h*.45f), Offset(w*.39f,h*.62f), 3f)
    // topology overlay
    for (i in 0..5) drawLine(Color(0x9978d5de), Offset(w*(.30f+i*.055f),h*.54f), Offset(w*(.35f+i*.05f),h*.43f), 1f)
    for (i in 0..12) { val px=w*(.28f+i*.032f); drawCircle(Color(0xffb4f5fa), Offset(px,h*(.55f-sin(i*.6f)*.08f)), 2.2f) }
    drawArc(Color(0xffffb84d), Offset(w*.285f,h*.40f), Size(w*.16f,h*.24f), 190f, 120f, false, style=Stroke(2f)); drawLine(Color(0xffd1dbe0), Offset(w*.65f,h*.55f), Offset(w*.78f,h*.58f), 3f); drawLine(Color(0xffd1dbe0), Offset(w*.61f,h*.58f), Offset(w*.80f,h*.64f), 3f)
}

@Composable private fun Inspector(selected: String, onSelected: (String) -> Unit) {
    val items = listOf("Motorcycle","  Body","  Frame","  Engine","  Wheels","  Tires","  Suspension","  Exhaust","  Handlebar","  Seat","  Lights","  Camera")
    Column(Modifier.width(275.dp).fillMaxHeight().background(Panel).verticalScroll(rememberScrollState())) {
        PanelHeader("OUTLINER", Icons.Default.AccountTree)
        items.forEach { item -> Row(Modifier.fillMaxWidth().clickable { onSelected(item.trim()) }.background(if (selected == item.trim()) Color(0xff1a3b4b) else Color.Transparent).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (item.trim() == "Motorcycle") Icons.Default.ExpandMore else Icons.Default.ViewInAr, null, tint = if (selected == item.trim()) Cyan else Muted, modifier = Modifier.size(14.dp)); Text(item, color = if (selected == item.trim()) Color.White else Muted, fontSize = 10.sp) } }
        Divider(color = Line, modifier = Modifier.padding(vertical = 5.dp)); PanelHeader("PROPERTIES", Icons.Default.Tune); PropertySection("Transform") { PropertyGrid() }; PropertySection("Material Preview") { ColorRow("Base Color", Color(0xff3d788c)); SliderRow("Metallic", "0.86"); SliderRow("Roughness", "0.24"); SliderRow("Normal", "0.18"); SliderRow("AO", "0.62"); SliderRow("Emission", "0.00"); SliderRow("Clearcoat", "0.72"); SliderRow("Transparency", "0.00") }
    }
}
@Composable private fun PanelHeader(title: String, icon: ImageVector) { Row(Modifier.fillMaxWidth().background(Color(0xff17232e)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Cyan, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(7.dp)); Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Icon(Icons.Default.MoreHoriz, null, tint = Muted, modifier = Modifier.size(16.dp)) } }
@Composable private fun PropertySection(title: String, content: @Composable () -> Unit) { Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) { Text(title.uppercase(), color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); content() } }
@Composable private fun PropertyGrid() { listOf("Dimensions" to "2.14 × 0.82 × 1.10 m", "Location" to "0.00, 0.00, 0.00", "Rotation" to "0.0°, 0.0°, 12.4°", "Scale" to "1.000, 1.000, 1.000").forEach { (a,b) -> Row(Modifier.padding(vertical = 3.dp)) { Text(a, color = Muted, fontSize = 9.sp, modifier = Modifier.width(70.dp)); Text(b, color = Color.White, fontSize = 9.sp) } } }
@Composable private fun ColorRow(label: String, color: Color) { Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = Muted, fontSize = 9.sp, modifier = Modifier.weight(1f)); Box(Modifier.size(54.dp,14.dp).background(color, RoundedCornerShape(3.dp)).border(1.dp, Color.White.copy(.22f), RoundedCornerShape(3.dp))) } }
@Composable private fun SliderRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = Muted, fontSize = 9.sp, modifier = Modifier.width(74.dp)); Box(Modifier.weight(1f).height(3.dp).background(Color(0xff304551), RoundedCornerShape(4.dp))) { Box(Modifier.fillMaxWidth(if (value.toFloatOrNull()?.coerceIn(0f,1f) ?: .5f)).height(3.dp).background(Cyan, RoundedCornerShape(4.dp))) }; Spacer(Modifier.width(7.dp)); Text(value, color = Color.White, fontSize = 9.sp) } }

@Composable private fun Timeline(frame: Int, onFrame: (Int) -> Unit, playing: Boolean, onPlay: () -> Unit) { Column(Modifier.height(117.dp).fillMaxWidth().background(Color(0xff0d151e))) { Row(Modifier.height(39.dp).fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { onFrame((frame-1).coerceAtLeast(1)) }) { Icon(Icons.Default.SkipPrevious, null, tint = Muted) }; IconButton(onClick = onPlay) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Cyan) }; IconButton(onClick = { onFrame(frame+1) }) { Icon(Icons.Default.SkipNext, null, tint = Muted) }; Text("FRAME", color = Muted, fontSize = 9.sp); Text(" $frame", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(15.dp)); Text("24 fps", color = Muted, fontSize = 9.sp); Spacer(Modifier.weight(1f)); Text("Auto Keyframe", color = Muted, fontSize = 9.sp); Spacer(Modifier.width(16.dp)); Text("Snap", color = Muted, fontSize = 9.sp); Icon(Icons.Default.Settings, null, tint = Muted, modifier = Modifier.padding(start = 14.dp).size(16.dp)) }; Row(Modifier.fillMaxWidth().weight(1f).horizontalScroll(rememberScrollState())) { Column(Modifier.width(145.dp).fillMaxHeight().background(Color(0xff121d27)).padding(start=12.dp)) { Text("ANIMATION", color=Cyan, fontSize=9.sp, fontWeight=FontWeight.Bold); Text("Motorcycle • Body", color=Color.White, fontSize=9.sp, modifier=Modifier.padding(top=9.dp)); Text("  Location / Rotation", color=Muted, fontSize=8.sp, modifier=Modifier.padding(top=6.dp)) }; Canvas(Modifier.width(1050.dp).fillMaxHeight()) { for (i in 0..72) { val x=i*40f; drawLine(if(i*5==frame) Cyan else Color(0xff263744), Offset(x,0f), Offset(x,size.height), if(i*5==frame) 2f else 1f); if(i%5==0) drawContext.canvas.nativeCanvas.drawText("${i*5}",x+2,14f,android.graphics.Paint().apply{color=android.graphics.Color.LTGRAY;textSize=18f}) }; drawLine(Cyan, Offset(frame*8f,0f), Offset(frame*8f,size.height), 2f); drawCircle(Color(0xffffbd59), Offset(220f,42f), 4f); drawCircle(Color(0xffffbd59), Offset(480f,42f), 4f) } } } }
