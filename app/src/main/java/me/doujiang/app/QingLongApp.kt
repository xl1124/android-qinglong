package me.doujiang.app

import android.app.Application
import android.os.Process
import me.doujiang.app.data.local.LocalStorage
import me.doujiang.app.data.repository.QingLongRepository
import java.io.FileWriter

class QingLongApp : Application() {

    lateinit var localStorage: LocalStorage
        private set

    lateinit var repository: QingLongRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        localStorage = LocalStorage(this)
        repository = QingLongRepository(localStorage)

        // 全局异常捕获 - 崩溃时将错误写入文件
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                val file = FileWriter(filesDir.resolve("crash.log"), true)
                file.write("=== CRASH at ${System.currentTimeMillis()} ===\n")
                file.write("Android: ${android.os.Build.VERSION.SDK_INT}, ${android.os.Build.MODEL}\n")
                file.write("${e.javaClass.name}: ${e.message}\n")
                e.stackTrace.forEach { file.write("  at $it\n") }
                file.write("\n")
                file.close()
            } catch (_: Exception) {}
            Process.killProcess(Process.myPid())
        }
    }

    companion object {
        @Volatile
        private var instance: QingLongApp? = null

        fun getInstance(): QingLongApp {
            return instance!!
        }
    }
}
