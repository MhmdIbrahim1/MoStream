package com.lagradost.cloudstream3.ui.settings.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.RepositoryItemBinding
import com.lagradost.cloudstream3.databinding.RepositoryItemTvBinding
import com.lagradost.cloudstream3.plugins.RepositoryManager.PREBUILT_REPOSITORIES
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.isTrueTvSettings

class RepoAdapter(
    val isSetup: Boolean,
    val clickCallback: RepoAdapter.(RepositoryData) -> Unit,
    val imageClickCallback: RepoAdapter.(RepositoryData) -> Unit,
    /** In setup mode the trash icons will be replaced with download icons */
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val repositories: MutableList<RepositoryData> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (isTrueTvSettings()) RepositoryItemTvBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ) else RepositoryItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )  //R.layout.repository_item_tv else R.layout.repository_item
        return RepoViewHolder(
            layout
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is RepoViewHolder -> {
                holder.bind(repositories[position])
            }
        }
    }

    // Clear glide image because setImageResource doesn't override
//    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
//        holder.itemView.entry_icon?.let { repoIcon ->
//            GlideApp.with(repoIcon).clear(repoIcon)
//        }
//        super.onViewRecycled(holder)
//    }

    override fun getItemCount(): Int {
        return repositories.size
    }

    fun updateList(newList: Array<RepositoryData>) {
        val diffResult = DiffUtil.calculateDiff(
            RepoDiffCallback(this.repositories, newList)
        )

        repositories.clear()
        repositories.addAll(newList)

        diffResult.dispatchUpdatesTo(this)
    }

    inner class RepoViewHolder(
        val binding: ViewBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            repositoryData: RepositoryData
        ) {
            val isPrebuilt = PREBUILT_REPOSITORIES.contains(repositoryData)
            val drawable = R.drawable.netflix_download
//                if (isSetup) R.drawable.netflix_download else R.drawable.ic_baseline_delete_outline_24
            // hide delete button if prebuilt repo

            when (binding) {
                is RepositoryItemTvBinding -> {
                    binding.apply {
                        // Only show icon if in setup and not a prebuilt repo.
                        if (isSetup && isPrebuilt) {
                            actionButton.setImageResource(drawable)
                            actionButton.visibility = View.GONE
                        } else {
                            actionButton.visibility = View.GONE
                        }

                        actionButton.setOnClickListener {
                            imageClickCallback(repositoryData)
                            actionButton.visibility = View.GONE
                        }


                        repositoryItemRoot.setOnLongClickListener {
                            val clipboardManager =
                                CommonActivity.activity?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager?
                            clipboardManager?.setPrimaryClip(ClipData.newPlainText("RepoUrl", repositoryData.url))
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                CommonActivity.showToast(
                                    R.string.repoCopied,
                                    Toast.LENGTH_SHORT
                                )
                            }
                            return@setOnLongClickListener true
                        }

                        repositoryItemRoot.setOnClickListener {

                            clickCallback(repositoryData)
                        }
                        mainText.text = repositoryData.name
                        subText.text = repositoryData.url
                    }
                }

                is RepositoryItemBinding -> {
                    binding.apply {
                        // Only show icon if in setup, or if it isn't a prebuilt repo.
                        if (isSetup || isPrebuilt) {
                            actionButton.setImageResource(drawable)
                            actionButton.visibility = View.GONE
                        } else {
                            actionButton.visibility = View.GONE
                        }

                        actionButton.setOnClickListener {
                            imageClickCallback(repositoryData)
                        }

                        repositoryItemRoot.setOnClickListener {
                            clickCallback(repositoryData)
                        }
                        mainText.text = repositoryData.name
                        subText.text = repositoryData.url
                    }
                }
            }
        }
    }
}

class RepoDiffCallback(
    private val oldList: List<RepositoryData>,
    private val newList: Array<RepositoryData>
) :
    DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].url == newList[newItemPosition].url

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition] == newList[newItemPosition]
}