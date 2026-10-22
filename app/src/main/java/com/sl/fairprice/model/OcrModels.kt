package com.sl.fairprice.model

data class TextBox(val left:Int,val top:Int,val right:Int,val bottom:Int)
data class OcrWord(val text:String,val confidence:Float,val box:TextBox)
data class OcrResult(
 val text:String,
 val words:List<OcrWord>,
 val meanConfidence:Int,
 val elapsedMs:Long
)
