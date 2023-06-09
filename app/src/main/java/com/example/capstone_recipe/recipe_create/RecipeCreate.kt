package com.example.capstone_recipe.recipe_create

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.capstone_recipe.Preference
import com.example.capstone_recipe.R
import com.example.capstone_recipe.recipe_create.create_fragments.RecipeCreateComplete
import com.example.capstone_recipe.recipe_create.create_fragments.RecipeCreateStepFirst
import com.example.capstone_recipe.recipe_create.create_fragments.RecipeCreateStepSecond
import com.example.capstone_recipe.recipe_create.create_fragments.RecipeCreateStepThird
import com.example.capstone_recipe.data_class.*
import com.example.capstone_recipe.databinding.ActivityRecipeCreateBinding
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat

/** *modifyMode -> 레시피 수정 모드 : 레시피 아이디 받아서 해당 레시피 를 수정함 */
class RecipeCreate() : AppCompatActivity(){
    private val binding by lazy { ActivityRecipeCreateBinding.inflate(layoutInflater) }
    private val storage = FirebaseStorage.getInstance()
    private val db = Firebase.database("https://knu-capstone-f9f55-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val defaultImageUri by lazy { Uri.parse("android.resource://$packageName/${R.drawable.default_recipe_main_image}") }  // 이미지 선택 안할 시 나오는 기본 이미지
    private val pref by lazy { Preference(context) }
    private lateinit var context:Context
    private var modifyMode = false
    private var recipeId = ""
    private var userId = ""

    private var recipeBasicInfo = RecipeBasicInfo()
    private var ingredientList = mutableListOf<Ingredient>()
    private var stepExplanationList = mutableListOf<String>("")
    private var stepImageList = mutableListOf<Uri?>(null)
    private var selectedMainImage: Uri? = null

    private var currentStep = STEP_FIRST

    private companion object{
        const val STEP_FIRST = 0
        const val STEP_SECOND = 1
        const val STEP_THIRD = 2
        const val STEP_COMPLETE = 3
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        context = binding.root.context
        userId = pref.getUserId()

        modifyMode = intent.getBooleanExtra("modifyMode", false)
        intent.removeExtra("modifyMode")
        Log.d("LOG_CHECK", "RecipeCreate :: onCreate() -> modifyMode : $modifyMode")
        if(modifyMode){
            recipeId = intent.getStringExtra("recipeId")!!
            lifecycleScope.launch(Dispatchers.IO) {
                val recipe = db.getReference("recipes").child(recipeId).get().await()
                recipeBasicInfo = recipe.child("basicInfo").getValue(RecipeBasicInfo::class.java)!!
                ingredientList = recipe.child("ingredient").getValue<MutableList<Ingredient>>()!!
                val recipeStepList = recipe.child("step").getValue<List<RecipeStep>>()!!
                val newExplanationList = MutableList<String>(recipeStepList.size) {""}
                val newImageList = MutableList<Uri?>(recipeStepList.size) { Uri.EMPTY }
                recipeStepList.mapIndexed { index, recipeStep ->
                    async {
                        newExplanationList[index] = recipeStep.explanation
                        newImageList[index] = getImageByPath(recipeStep.imagePath, "step")
                    }
                }.awaitAll()
                stepExplanationList = newExplanationList
                stepImageList = newImageList
                selectedMainImage = getImageByPath(recipeBasicInfo.mainImagePath, "main_image")
            }
        }

        binding.topPanel.btnBack.setOnClickListener { finish() }
        supportFragmentManager
            .beginTransaction()
            .add(R.id.mainFrame, RecipeCreateStepFirst())
            .addToBackStack(null)
            .commit()

        binding.btnNext.setOnClickListener {
            currentStep++
            checkCurrentStep(currentStep)
        }

        binding.btnPrev.setOnClickListener {
            currentStep--
            if(currentStep == STEP_FIRST) { binding.btnPrev.visibility = View.GONE }
            supportFragmentManager.popBackStack()
        }

        binding.topPanel.btnBack.setOnClickListener {// test
            finish()
        }

        Glide.with(this)
            .asGif()
            .load(R.drawable.progress)
            .into(binding.ivProgressImage)
    }

    /** * imagePath 값이 있으면 해당 이미지 가져옴. detailPath --> step || main_image 중 하나 택*/
    private suspend fun getImageByPath(imagePath: String, detailPath: String): Uri{
        val imageRef = storage.getReference("recipe_image").child(recipeId).child(detailPath)
        return if(imagePath.isNotEmpty()){ imageRef.child(imagePath).downloadUrl.await() }
                else { defaultImageUri }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return if (supportFragmentManager.backStackEntryCount in 2..3) {
                currentStep--
                if(currentStep == STEP_FIRST) { binding.btnPrev.visibility = View.GONE }
                supportFragmentManager.popBackStack()
                true
            }
            else{
                finish()
                true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.animation_enter_from_right,
                R.anim.animation_exit_to_left,
                R.anim.animation_enter_from_left,
                R.anim.animation_exit_to_right
            )
            .add(R.id.mainFrame, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun checkCurrentStep(currentStep:Int){
        when(currentStep){
            STEP_FIRST -> {
                binding.btnPrev.visibility = View.GONE
                replaceFragment(RecipeCreateStepFirst())
            }
            STEP_SECOND -> {
                binding.btnPrev.visibility = View.VISIBLE
                binding.btnNext.visibility = View.VISIBLE

                val recipeCreateStepFirst = (supportFragmentManager.findFragmentById(R.id.mainFrame)!!) as RecipeCreateStepFirst
                ingredientList = recipeCreateStepFirst.ingredientList
                recipeBasicInfo = recipeCreateStepFirst.getRecipeBasicInfo()

                replaceFragment(RecipeCreateStepSecond(stepExplanationList, stepImageList))
            }
            STEP_THIRD -> {
                binding.btnPrev.visibility = View.VISIBLE

                val recipeCreateStepSecond = (supportFragmentManager.findFragmentById(R.id.mainFrame)!!) as RecipeCreateStepSecond
                stepExplanationList =  recipeCreateStepSecond.stepExplanationList
                stepImageList = recipeCreateStepSecond.stepImageList

                replaceFragment(RecipeCreateStepThird(stepImageList))
            }
            STEP_COMPLETE -> {
                val recipeCreateStepThird = (supportFragmentManager.findFragmentById(R.id.mainFrame)!!) as RecipeCreateStepThird
                recipeBasicInfo.shareOption = recipeCreateStepThird.shareOption
                selectedMainImage= recipeCreateStepThird.mainImageUri

                hideKeyboard()
                startUpload()
                makeUpRecipeInfo()
                Log.d("LOG_CHECK", "RecipeCreate :: checkCurrentStep() ->\nbasic : $recipeBasicInfo\ningredient : $ingredientList" +
                        "\nimage : $stepImageList")
            }
        }
    }

    private fun startUpload(){
        binding.topPanel.root.visibility = View.GONE
        binding.mainFrame.visibility = View.GONE
        binding.bottomPanel.visibility = View.GONE
        binding.progress.visibility = View.VISIBLE
    }

    private fun endUpload(){
        Log.d("LOG_CHECK", "RecipeCreate :: endUpload() -> ")
        binding.progress.visibility = View.GONE
        binding.mainFrame.visibility = View.VISIBLE
        replaceFragment(RecipeCreateComplete(recipeId))
    }

    @SuppressLint("SimpleDateFormat")
    private fun makeRecipeId(): String{
        val currentTime = SimpleDateFormat("yyyyMMddHHmmss")
            .format(System.currentTimeMillis()) // 2023 04 09 22 48  형식으로 변경
        return currentTime + "_" + userId + "_" + recipeBasicInfo.title
    }

    private fun makeUpRecipeInfo(){ // db에 올리기 전 모든 정보를 정리
        recipeId =
            if(modifyMode) { recipeId }
            else { makeRecipeId() }
        recipeBasicInfo.id = recipeId
        Log.d("LOG_CHECK", "RecipeCreate :: makeUpRecipeInfo() -> recipeBasicInfo.id : ${recipeBasicInfo.id}")
        recipeBasicInfo.mainImagePath = uriToPath(selectedMainImage) // 스토리지 경로로 변환
        uploadToUserDB()
    }

    private fun uploadToUserDB(){ // 유저 정보 업데이트 -> 레시피 아이디 추가
        db.getReference("users") // 유저가 올린 레시피들
            .child(userId)
            .child("uploadRecipe")   // root/users/$userid/recipes/...
            .child(recipeId)
            .setValue(recipeId)
            .addOnCompleteListener {     // 추가 후 처리 = 레시피 정보 등록
                uploadToRecipeDB()
            }
            .addOnFailureListener {
                Log.d("LOG_CHECK", "fail :: $it")
            }
    }

    private fun uploadToRecipeDB(){ // 레시피 정보 업데이트
        val recipePath = db.getReference("recipes").child(recipeId)
        val basicInfoPath = recipePath.child("basicInfo")
        val ingredientPath = recipePath.child("ingredient")
        val stepPath = recipePath.child("step")
        val favoritePeople = recipePath.child("favoritePeople")

        basicInfoPath.setValue(recipeBasicInfo)
        ingredientPath.setValue(ingredientList)
        favoritePeople.setValue("")

        val stepList = mutableListOf<RecipeStep>()
        for(i in 0 until stepExplanationList.size){ // 단계 설명 + 이미지 (null 가능)
            stepList.add(
                RecipeStep(
                    explanation = stepExplanationList[i],       // null 불가
                    imagePath = uriToPath(stepImageList[i], i)) // null 가능
            )
        }
        stepPath.setValue(stepList)
            .addOnCompleteListener {
                uploadToStorage(stepList)
            }

    }

    @SuppressLint("SimpleDateFormat")
    fun uriToPath(uri: Uri?, step:Int = -1): String { // 스텝 별 이미지
        if(uri == null) { return "" }
        Log.d("LOG_CHECK", "RecipeCreate :: uriToPath() -> uri : $uri")
        val mimeType = contentResolver?.getType(uri) ?: "/none" //마임타입 ex) images/jpeg
        val ext = mimeType.split("/")[1] //확장자 ex) jpeg

        return if(step == -1) { "${userId}_main.$ext" }  // step이 -1인 경우 메인 이미지로 간주
        else { "${userId}_step_$step.$ext" }             // step 이미지인 경우 각 스텝 번호를 이미지 경로에 부여
    }

    private fun uploadToStorage(stepList: List<RecipeStep>){ // 이미지 정보 업데이트
        val recipeRef = storage.getReference("recipe_image").child(recipeId)

        val stepRef = recipeRef.child("step")
        for(i in 0 until stepImageList.size){
            if(stepImageList[i] != null){
                stepRef
                    .child(stepList[i].imagePath)
                    .putFile(stepImageList[i]!!)
                    .addOnSuccessListener { Log.d("LOG_CHECK", "RecipeCreate :: uploadToStorage() -> complete step $i upload") }
                    .addOnFailureListener { Log.d("LOG_CHECK", "RecipeCreate :: uploadToStorage() -> fail $it") }
            }
        }
        if(selectedMainImage != null){
            recipeRef
                .child("main_image")
                .child(recipeBasicInfo.mainImagePath!!)
                .putFile(selectedMainImage!!)
                .addOnSuccessListener {
                    Log.d("LOG_CHECK", "RecipeCreate :: uploadToStorage() -> complete main upload ")
                    endUpload()
                }
                .addOnFailureListener { Log.d("LOG_CHECK", "RecipeCreate :: uploadToStorage() -> fail $it") }
        }
        else{ endUpload() }
    }


    /** *현재 키보드가 올라와 있는 상태면 강제로 내림 */
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (currentFocus != null) { imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0) }
    }
}