package com.myapps.wearenginepractice.ui.fragments

import android.os.Bundle
import android.view.View
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.myapps.wearenginepractice.R
import com.myapps.wearenginepractice.db.ArticleDatabase
import com.myapps.wearenginepractice.repository.NewsRepository
import com.myapps.wearenginepractice.ui.NewsViewModel
import com.myapps.wearenginepractice.ui.NewsViewModelProviderFactory
import kotlinx.android.synthetic.main.fragment_article.*

class ArticleFragment : Fragment(R.layout.fragment_article) {

    private lateinit var viewModel: NewsViewModel
    private val args: ArticleFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val newsRepository = NewsRepository(ArticleDatabase(requireContext()))
        val viewModelProviderFactory = NewsViewModelProviderFactory(newsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(NewsViewModel::class.java)

        val article = args.article
        val newsUrl = args.newsUrl

        webView.apply {
            webViewClient = WebViewClient()
            if (article?.url?.isNotEmpty() == true){
                loadUrl(article.url)
            }else{
                if (newsUrl != null) {
                    loadUrl(newsUrl)
                }
            }
        }

        fab.setOnClickListener {
            if (article != null) {
                viewModel.saveArticle(article)
            }
            Snackbar.make(view, "Article saved successfully", Snackbar.LENGTH_SHORT).show()
        }
    }
}