package com.modori.colorpicker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.modori.colorpicker.Api.RandomImage
import com.modori.colorpicker.Model.RandomImageModel
import com.modori.colorpicker.RA.ColorAdapter
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory as BitmapFactory1


class MainActivity : AppCompatActivity() {

    var vibrantSwatch: Palette.Swatch? = null
    var darkMutedSwatch: Palette.Swatch? = null
    var darkVibrantSwatch: Palette.Swatch? = null
    var dominantSwatch: Palette.Swatch? = null
    var lightMutedSwatch: Palette.Swatch? = null
    var lightVibrantSwatch: Palette.Swatch? = null
    var mutedSwatch: Palette.Swatch? = null

    var photoId:String = "eee"
    var photoBitmap:Bitmap? = null
    var colorList:IntArray? = null
    var retrofit:Retrofit? = null


    private val PICTURE_REQUEST_CODE: Int = 123


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        retrofit = Retrofit.Builder()
            .baseUrl("https://api.unsplash.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        if(savedInstanceState != null){
            val mPhotoId:String = savedInstanceState.getString("photoId")
            getPhotoById(mPhotoId)
        }else{
            getRandomPhoto()

        }




        shareBtn.setOnClickListener {
            val intent = Intent(this, ScreenshotActivity::class.java)

            if(photoId != "eee"){
//                val stream = ByteArrayOutputStream()
//                photoBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
//                val bytes:ByteArray = stream.toByteArray()


                intent.putExtra("photoId", photoId)
                intent.putExtra("colorList", colorList)
                startActivity(intent)

            }else{
                Toast.makeText(this,"사진을 받아오고 있습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        refreshBtn.setOnClickListener {
            getRandomPhoto()
        }

        openGallery.setOnClickListener {
            val getFromGallery = Intent(Intent.ACTION_PICK)
            getFromGallery.type = "image/*"
            getFromGallery.putExtra(Intent.ACTION_GET_CONTENT, true)
            getFromGallery.type = MediaStore.Images.Media.CONTENT_TYPE
            startActivityForResult(Intent.createChooser(getFromGallery, "Select Picture"), PICTURE_REQUEST_CODE)

        }


    }

    private fun getPhotoById(mPhotoId:String){

        Log.d("getPhotoByID", mPhotoId)

        retrofit = Retrofit.Builder()
            .baseUrl("https://api.unsplash.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit!!.create(RandomImage::class.java)
        val call = service.getPhotoById(mPhotoId)

        call.enqueue(object :Callback<RandomImageModel>{
            override fun onFailure(call: Call<RandomImageModel>, t: Throwable) {
                Log.d("IDsearch 실패", t.message)
            }

            override fun onResponse(call: Call<RandomImageModel>, response: Response<RandomImageModel>) {
                Log.d("받아온 값", response.body().toString())
                Glide.with(applicationContext).asBitmap().load(response.body()!!.urls!!.regular)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            model: Any?,
                            target: Target<Bitmap>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            setImageView(resource)
                            photoBitmap = resource
                            photoId = response.body()!!.id
                            createPaletteAsync(resource)
                            return true
                        }
                    }).into(imageview)
            }
        })

    }

    private fun getRandomPhoto() {
        retrofit = Retrofit.Builder()
            .baseUrl("https://api.unsplash.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit!!.create(RandomImage::class.java)
        val call = service.getRandomPhoto()
        call.enqueue(object : Callback<RandomImageModel> {
            override fun onResponse(call: Call<RandomImageModel>, response: Response<RandomImageModel>) {
                if (response.isSuccessful) {

                    Glide.with(applicationContext).asBitmap().load(response.body()!!.urls!!.regular)
                        .listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Bitmap>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                return false
                            }

                            override fun onResourceReady(
                                resource: Bitmap,
                                model: Any?,
                                target: Target<Bitmap>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                setImageView(resource)
                                photoId = response.body()!!.id
                                photoBitmap = resource
                                Log.d("초기 id", photoId)
                                createPaletteAsync(resource)
                                return true
                            }
                        }).into(imageview)


                }
            }

            override fun onFailure(call: Call<RandomImageModel>, t: Throwable) {
                Log.d("통신 실패 사유", t.message)
            }
        })

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("photoId", photoId)
        outState.putString("photoId", photoId)

    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICTURE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val mDAta = data.data
                val uri: Uri? = mDAta

                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                imageview.setImageBitmap(bitmap)
                createPaletteAsync(bitmap)
            }
        }

    }


    private fun createPaletteAsync(bitmap: Bitmap) {
        Palette.from(bitmap).generate {

            vibrantSwatch = it?.vibrantSwatch
            darkMutedSwatch = it?.darkMutedSwatch
            darkVibrantSwatch = it?.darkVibrantSwatch
            dominantSwatch = it?.dominantSwatch
            lightMutedSwatch = it?.lightMutedSwatch
            lightVibrantSwatch = it?.lightVibrantSwatch
            mutedSwatch = it?.mutedSwatch

            //colorData = ArrayList()
            val colorSet: MutableSet<Int> = mutableSetOf()


            if (vibrantSwatch != null) {
                colorSet.add(vibrantSwatch!!.rgb)


            }

            if (darkMutedSwatch != null) {
                colorSet.add(darkMutedSwatch!!.rgb)


            }

            if (darkVibrantSwatch != null) {
                colorSet.add(darkVibrantSwatch!!.rgb)


            }

            if (dominantSwatch != null) {
                colorSet.add(dominantSwatch!!.rgb)

            }

            if (lightMutedSwatch != null) {
                colorSet.add(lightMutedSwatch!!.rgb)

            }
            if (lightVibrantSwatch != null) {
                colorSet.add(lightVibrantSwatch!!.rgb)

            }

            if (mutedSwatch != null) {
                colorSet.add(mutedSwatch!!.rgb)

            }

            setRecyclerView(colorSet)




        }
    }

    private fun setImageView(bitmap: Bitmap){
        YoYo.with(Techniques.FadeIn)
            .duration(300)
            .repeat(0)
            .playOn(imageview)
        imageview.setImageBitmap(bitmap)
    }

    private fun setRecyclerView(colorSet:Set<Int>){
        val adapter = ColorAdapter(colorSet.toList(), this)
        colorList = colorSet.toIntArray()
        Log.d("색류", colorSet.toString())
        colorsRV.layoutManager = LinearLayoutManager(this)
        colorsRV.adapter = adapter


        YoYo.with(Techniques.FadeIn)
            .duration(300)
            .repeat(0)
            .playOn(colorsRV)

        adapter.notifyDataSetChanged()

    }



}
