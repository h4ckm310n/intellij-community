// MODE: return
val x = run foo@{
    println("foo")
    1/*<# ^|[KotlinLambdasHintsProvider.kt:32]foo #>*/
}