package id.co.bankntbsyariah.lakupandai.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.Constants
import id.co.bankntbsyariah.lakupandai.databinding.ActivityCheckUpdateBinding
import id.co.bankntbsyariah.lakupandai.iface.ArrestCallerImpl
import id.co.bankntbsyariah.lakupandai.iface.StorageImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class CheckUpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckUpdateBinding
    private var processStage = 0
    private lateinit var processHandler: Handler

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        processHandler = Handler(mainLooper)
        binding = ActivityCheckUpdateBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)

        nextProcessStage()
    }

    private fun nextProcessStage() {
        processHandler.postDelayed(
            {
                displayProcessStage(processStage)
                if (processStage < Constants.MAX_CHECK_UPDATE_PROCESS_STAGE) nextProcessStage()
            }, 1800)
    }

    private fun displayProcessStage(stage: Int) {
        when (stage) {
            0 -> {
                binding.versionText.text = getString(R.string.process_check_update)
                processStage += 1
            }
            1 -> {
                binding.versionText.text = getString(R.string.process_check_menu_version)
                lifecycleScope.launch {
                    val menuVer = StorageImpl(applicationContext).fetchVersion()
                    val serverVer =
                        withContext(Dispatchers.IO) {
                            ArrestCallerImpl(OkHttpClient()).fetchVersion()?.toString()?.toIntOrNull() ?: 0
                        }
                    processStage += if (menuVer < serverVer) {
                        StorageImpl(applicationContext).updateVersion(serverVer)
                        1
                    } else {
                        2
                    }
                }
            }
            2 -> {
                binding.versionText.text = getString(R.string.process_downloading_menu)
                lifecycleScope.launch {
                    val rootMenuId =
                        withContext(Dispatchers.IO) {
                            ArrestCallerImpl(OkHttpClient()).fetchRootMenuId().toString()
                        }
                    StorageImpl(applicationContext).updateRootMenuId(rootMenuId)
                    processStage += 1
                }
            }
            3 -> {
                binding.versionText.text = getString(R.string.status_menu_updated)
                processStage += 1
            }
            4 -> {
                binding.versionText.text = getString(R.string.process_downloading_update)
                processStage += 1
            }
            5 -> {
                binding.progressCircular.visibility = View.GONE
                val versionName = packageManager.getPackageInfo(packageName, 0).versionName
                binding.versionText.text = getString(R.string.debug_version, versionName)
                processStage += 1
            }
            else -> {
                processStage = Constants.MAX_CHECK_UPDATE_PROCESS_STAGE
                navigateToLoginActivity()
            }
        }
    }

    private fun navigateToLoginActivity(){
        finish()
        startActivity(Intent(this, FormActivity::class.java).apply{
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(Constants.KEY_FORM_ID, "AU00001")
        })
    }
}
