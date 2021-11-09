package com.myapps.wearenginepractice.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.ParcelFileDescriptor
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapps.wearenginepractice.models.Article
import com.myapps.wearenginepractice.models.NewsResponse
import com.myapps.wearenginepractice.repository.NewsRepository
import com.myapps.wearenginepractice.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.File
import java.lang.Exception
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsBuildBitmapOption
import com.huawei.hms.ml.scan.HmsScan
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions


class NewsViewModel(
    private val newsRepository: NewsRepository
) : ViewModel() {

    val breakingNews: MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    private val breakingNewsPage = 1

    val searchNews: MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    private val searchNewsPage = 1

    init {
        getBreakingNews("us")
    }

    private fun getBreakingNews(countryCode: String) = viewModelScope.launch {
        breakingNews.postValue(Resource.Loading())
        val response = newsRepository.getBreakingNews(countryCode, breakingNewsPage)
        breakingNews.postValue(handleBreakingNewsResponse(response))
    }

    fun searchNews(searchQuery: String) = viewModelScope.launch {
        searchNews.postValue(Resource.Loading())
        val response = newsRepository.searchNews(searchQuery, searchNewsPage)
        searchNews.postValue(handleSearchNewsResponse(response))
    }

    private fun handleBreakingNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                return Resource.Success(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private fun handleSearchNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                return Resource.Success(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    fun saveArticle(article: Article) = viewModelScope.launch {
        newsRepository.upsert(article)
    }

    fun getSavedNews() = newsRepository.getSavedNews()

    fun deleteArticle(article: Article) = viewModelScope.launch {
        newsRepository.deleteArticle(article)
    }

    fun generateQrCode(context: Context, filePath: Uri): Bitmap {
        val options = HmsBuildBitmapOption.Creator().setBitmapBackgroundColor(Color.TRANSPARENT)
            .setBitmapColor(Color.BLACK).setBitmapMargin(3).create()

        val qrCodeString = convertPdfBitmapToQrCodeString(context, filePath)

        return ScanUtil.buildBitmap(qrCodeString, HmsScan.QRCODE_SCAN_TYPE, 300, 300, options)

    }

    private fun convertPdfBitmapToQrCodeString(context: Context, filePath: Uri): String {
        val options = HmsScanAnalyzerOptions.Creator().setHmsScanTypes(
            HmsScan.QRCODE_SCAN_TYPE,
            HmsScan.DATAMATRIX_SCAN_TYPE
        ).setPhotoMode(true).create()

        val bitmap = pdfToBitmap(context, filePath)
        val hmsScans = ScanUtil.decodeWithBitmap(context, bitmap[0], options)

        return hmsScans[0].getOriginalValue()

    }

    fun convertPdfBitmapToQrCodeResult(context: Context, filePath: Uri): String {
        val options = HmsScanAnalyzerOptions.Creator().setHmsScanTypes(
            HmsScan.QRCODE_SCAN_TYPE,
            HmsScan.DATAMATRIX_SCAN_TYPE
        ).setPhotoMode(true).create()

        val bitmap = pdfToBitmap(context, filePath)
        val hmsScans = ScanUtil.decodeWithBitmap(context, bitmap[0], options)

        return hmsScans[0].getShowResult()

    }

    private fun pdfToBitmap(context: Context, filePath: Uri): ArrayList<Bitmap> {
        val bitmaps: ArrayList<Bitmap> = ArrayList()
        try {
            val pdfFile = getFileFromUri(context, filePath)
            val renderer =
                PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY))
            var bitmap: Bitmap
            val pageCount = renderer.pageCount
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width: Int = context.resources.displayMetrics.densityDpi / 72 * page.width
                val height: Int = context.resources.displayMetrics.densityDpi / 72 * page.height
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)

                // close the page
                page.close()
            }

            // close the renderer
            renderer.close()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return bitmaps
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        if (uri.path == null) {
            return null
        }
        var realPath = String()
        val databaseUri: Uri
        val selection: String?
        val selectionArgs: Array<String>?
        if (uri.path!!.contains("/document/image:")) {
            databaseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            selection = "_id=?"
            selectionArgs = arrayOf(DocumentsContract.getDocumentId(uri).split(":")[1])
        } else {
            databaseUri = uri
            selection = null
            selectionArgs = null
        }
        try {
            val column = "_data"
            val projection = arrayOf(column)
            val cursor = context.contentResolver?.query(
                databaseUri,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.let {
                if (it.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    realPath = cursor.getString(columnIndex)
                }
                cursor.close()
            }
        } catch (e: Exception) {
            Log.i("GetFileUri Exception:", e.message ?: "")
        }
        val path = if (realPath.isNotEmpty()) realPath else {
            when {
                uri.path!!.contains("/document/raw:") -> uri.path!!.replace(
                    "/document/raw:",
                    ""
                )
                uri.path!!.contains("/document/primary:") -> uri.path!!.replace(
                    "/document/primary:",
                    "/storage/emulated/0/"
                )
                else -> return null
            }
        }
        return File(path)
    }
}