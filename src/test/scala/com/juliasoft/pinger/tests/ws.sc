import scala.annotation.tailrec

val a: Array[Int] = Array(2,3,1,5)
val b = a.sorted

@tailrec def findHole(v: Int, idx: Int, arr: Array[Int]): Int = {
  if (v == arr(idx)) findHole(1 + v, 1 + idx, arr) else v
}

findHole(b(0), 0, b)

/*
var index = 0
var value = b(0)
while(value == b(index)) {
  index += 1
  value += 1
}
value
*/