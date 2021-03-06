package com.serhatyurdakul.todo.app.ui.main

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.serhatyurdakul.todo.R
import com.serhatyurdakul.todo.app.data.local.category.CategoryEntity
import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity
import com.serhatyurdakul.todo.app.ui.helper.Validator
import com.serhatyurdakul.todo.app.ui.main.adapter.TodoAdapter
import com.serhatyurdakul.todo.app.ui.main.callback.CategoryClickEvent
import com.serhatyurdakul.todo.app.ui.main.callback.TodoClickEvent
import com.serhatyurdakul.todo.databinding.PromptCategoryBinding
import com.serhatyurdakul.todo.databinding.PromptTodoBinding
import com.serhatyurdakul.todo.util.helper.FormatUtil
import com.serhatyurdakul.todo.util.helper.Toaster
import com.serhatyurdakul.todo.util.lib.firebase.callback.CategoryCallback
import com.serhatyurdakul.todo.util.lib.firebase.callback.TodoCallback
import com.serhatyurdakul.todo.util.lib.firebase.callback.TodoListCallback
import dev.sasikanth.colorsheet.ColorSheet
import java.util.*


class TodoDialogFragment(private var todoAdapter: TodoAdapter, private  var title: String,var mainActivity: MainActivity) : DialogFragment() {
    private var mRecyclerView: RecyclerView? = null
    var mDialog : TodoDialogFragment? = null
    var dialogAddTodo: AlertDialog?= null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mDialog=this

        mRecyclerView =  RecyclerView(mainActivity)
        mRecyclerView!!.layoutManager=LinearLayoutManager(mainActivity)
        mRecyclerView!!.adapter=todoAdapter
        todoAdapter.setListeners(object : TodoClickEvent {
            override fun onClickTodo(todo: TodoEntity, action: String, position: Int) {
                when (action) {
                    TodoClickEvent.ACTION_COMPLETE -> toggleMarkAsComplete(todo, position)
                    TodoClickEvent.ACTION_DETAILS -> showDetails(todo)
                    TodoClickEvent.ACTION_EDIT -> editTodo(todo, position)
                    TodoClickEvent.ACTION_DELETE -> deleteTodo(todo, position)
                }
            }
        }, object : CategoryClickEvent {
            override fun onClickCategory(category: CategoryEntity, action: String, position: Int) {
                when (action) {
                    CategoryClickEvent.ACTION_EDIT -> editCategory(category)
                    CategoryClickEvent.ACTION_DELETE -> deleteCategory(category, position)
                }
            }
        }

        )

      var alert =  AlertDialog.Builder(mainActivity)
        alert.setTitle(title)
        alert.setView(mRecyclerView)
        alert.setPositiveButton("Kapat") { dialogInterface: DialogInterface, i: Int ->
            dismiss()
           }
        alert.setNegativeButton("Görev Ekle") { dialogInterface: DialogInterface, i: Int ->
           addTodo()
        }
          return alert.create()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if(todoAdapter.getToDoCount()==0)
        {
            if(todoAdapter.getCategoryListArray().isEmpty())
                addCategory(true)
            else
                addTodo()

        }
        else
            super.show(manager, tag)
    }

    // add new todoItem with custom alert dialog
    private fun addTodo() {

        if (mainActivity.currentUserEntity == null) {
            mainActivity.signIn(mainActivity); return
        }


            val binding = DataBindingUtil.inflate<PromptTodoBinding>(
                layoutInflater, R.layout.prompt_todo, null, false
            )


            val adapterCategory = ArrayAdapter<String>(
                mainActivity,
                R.layout.dropdown_menu_popup_item,
                todoAdapter.getCategoryListArray()
            )

            binding.filledExposedDropdown.setAdapter(adapterCategory)
            binding.filledExposedDropdown.setOnClickListener {
                val imm =
                    mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)

            }
            binding.tilTodoCategory.setOnClickListener {
                val imm =
                    mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)

            }

            binding.tietTodoDate.text = SpannableStringBuilder(title)

            val myCalendar = Calendar.getInstance()
            val dateListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                myCalendar.set(Calendar.YEAR, year)
                myCalendar.set(Calendar.MONTH, month)
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val dateSelected = FormatUtil().formatDate(myCalendar.time, FormatUtil.dd_MMM_yyyy)
                binding.tietTodoDate.text = SpannableStringBuilder(dateSelected)

            }
            binding.tietTodoDate.setOnClickListener {
                DatePickerDialog(
                    mainActivity, dateListener, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        binding.tietTodoTime.text = SpannableStringBuilder("12:00")


        val timeListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            myCalendar.set(Calendar.MINUTE, minute)
            myCalendar.set(Calendar.SECOND, 0)
            val timeSelected = "$hourOfDay:$minute"
            binding.tietTodoTime.text = SpannableStringBuilder(timeSelected)

        }
        binding.tietTodoTime.setOnClickListener {
            TimePickerDialog(mainActivity,timeListener,myCalendar.get(Calendar.HOUR_OF_DAY),myCalendar.get(Calendar.MINUTE),true).show()
        }

             dialogAddTodo = AlertDialog.Builder(mainActivity)
                .setTitle(R.string.label_add_todo)
                .setView(binding.root)
                .setPositiveButton(R.string.label_add_todo) { _, _ ->


                    val todoTitle = binding.tietTodoTitle.text.toString()
                    val date = binding.tietTodoDate.text.toString()
                    val dateEpoch = myCalendar.timeInMillis
                    val category = binding.filledExposedDropdown.text.toString()
                    val todo = TodoEntity(
                        "",
                        todoTitle,
                        false,
                        date,
                        dateEpoch,
                        mainActivity.currentUserEntity?.id!!,
                        "",
                        category
                    )

                    mainActivity.remote.addTodo(todo, object : TodoCallback {
                        override fun onResponse(todoCurrent: TodoEntity?, error: String?) {

                            if (error == null) {
                                Toaster(mainActivity).showToast(mainActivity.getString(R.string.add_todo_success_message))
                                val intent = Intent(mainActivity,ReminderBroadcast::class.java)
                                intent.action = Gson().toJson(todo)
                                val pendingIntent = PendingIntent.getBroadcast(mainActivity,Random().nextInt(),intent,
                                    PendingIntent.FLAG_CANCEL_CURRENT
                                )
                                val alarmManager = mainActivity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                alarmManager.set(AlarmManager.RTC_WAKEUP,todo.dateEpoch,pendingIntent)


                            } else {
                                Toaster(mainActivity).showToast(error)
                            }
                        }
                    })

               //     mDialog?.dismiss()
                }
                .setNegativeButton(R.string.label_cancel) { _, _ -> }
                .create()

            dialogAddTodo?.setOnShowListener {
                dialogAddTodo?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
            }

        binding.filledExposedDropdown.setOnItemClickListener { _, _, position, _ ->
            if(todoAdapter.getCategoryListArray()[position]=="Kategori Ekle ")
            {
                addCategory(false)
               // dialog.dismiss()
            }

        }
            dialogAddTodo?.show()

            Validator.forceValidation(arrayOf(binding.tietTodoTitle, binding.tietTodoDate , binding.tietTodoTime), dialogAddTodo!!)


    }
    // mark a todoItem as complete/incomplete
    private fun toggleMarkAsComplete(todo: TodoEntity, position: Int) {
        if (mainActivity.currentUserEntity == null) {
            mainActivity.signIn(mainActivity); return
        }



        var successMessage = R.string.task_marked_as_completed_success_message
        if (todo.completed) successMessage = R.string.task_marked_as_incomplete_success_message
        todo.completed = !todo.completed
        mainActivity.remote.markTodoListAsComplete(arrayListOf(todo), object : TodoListCallback {
            override fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?) {

                if (error == null) {
                    Toaster(mainActivity).showToast(mainActivity.getString(successMessage))
                } else {
                    Toaster(mainActivity).showToast(error)
                }
            }
        })
        mDialog?.dismiss()
    }

    //delete a todoItem permanently
    private fun deleteTodo(todo: TodoEntity, position: Int) {
        AlertDialog.Builder(mainActivity)
            .setTitle(R.string.text_are_you_sure)
            .setMessage(R.string.todo_delete_warning)
            .setPositiveButton(R.string.label_delete) { _, _ ->

                mainActivity.remote.deleteTodo(todo, object : TodoCallback {
                    override fun onResponse(todo: TodoEntity?, error: String?) {

                        if (error == null) {
                            Toaster(mainActivity).showToast(mainActivity.getString(R.string.delete_todo_success_message))
                            //adapter.getTodoList().remove(todo)
                            //adapter.notifyDataSetChanged()
                        } else {
                            Toaster(mainActivity).showToast(error)
                        }
                    }
                })
                mDialog?.dismiss()
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()
            .show()
    }

    //delete a categoryItem permanently
    private fun deleteCategory(category: CategoryEntity, position: Int) {
        AlertDialog.Builder(mainActivity)
            .setTitle(R.string.text_are_you_sure)
            .setMessage(R.string.todo_delete_warning)
            .setPositiveButton(R.string.label_delete) { _, _ ->

                mainActivity.remote.deleteCategory(category, object : CategoryCallback {
                    override fun onResponse(category: CategoryEntity?, error: String?) {

                        if (error == null) {
                            Toaster(mainActivity).showToast(mainActivity.getString(R.string.delete_category_success_message))
                            //adapter.getTodoList().remove(todo)
                            //adapter.notifyDataSetChanged()
                        } else {
                            Toaster(mainActivity).showToast(error)
                        }
                    }
                })

                mDialog?.dismiss()
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()
            .show()
    }
    //show todoItem's details
    private fun showDetails(todo: TodoEntity) {
        val details = "Title: ${todo.todo}\nDate: ${todo.date}"
        AlertDialog.Builder(mainActivity)
            .setTitle(R.string.label_details)
            .setMessage(details)
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()
            .show()
        mDialog?.dismiss()
    }
    // edit an existing and incomplete todoItem
    private fun editTodo(todo: TodoEntity, position: Int) {

        val binding = DataBindingUtil.inflate<PromptTodoBinding>(
            layoutInflater, R.layout.prompt_todo, null, false
        )

        binding.tietTodoTitle.text = SpannableStringBuilder(todo.todo)
        binding.tietTodoDate.text = SpannableStringBuilder(todo.date)
        binding.tietTodoTitle.setSelection(todo.todo.length)
        val adapterCategory = ArrayAdapter<String>(
            mainActivity,
            R.layout.dropdown_menu_popup_item,
            todoAdapter.getCategoryListArray()
        )

        binding.filledExposedDropdown.setAdapter(adapterCategory)
        binding.filledExposedDropdown.setOnClickListener {
            val imm =
                mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)

        }
        binding.tilTodoCategory.setOnClickListener {
            val imm =
                mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)

        }
        val myCalendar = Calendar.getInstance()
        val dateListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val dateSelected = FormatUtil().formatDate(myCalendar.time, FormatUtil.dd_MMM_yyyy)
            binding.tietTodoDate.text = SpannableStringBuilder(dateSelected)

        }
        binding.tietTodoDate.setOnClickListener {
            DatePickerDialog(
                mainActivity, dateListener, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        var tempCalendar = Calendar.getInstance()
        tempCalendar.timeInMillis=todo.dateEpoch
        binding.tietTodoTime.text = SpannableStringBuilder(String.format("%02d:%02d", tempCalendar.get(Calendar.HOUR_OF_DAY),tempCalendar.get(Calendar.MINUTE)))



        val timeListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            myCalendar.set(Calendar.MINUTE, minute)
            myCalendar.set(Calendar.SECOND, 0)
            val timeSelected = "$hourOfDay:$minute"
            binding.tietTodoTime.text = SpannableStringBuilder(timeSelected)

        }
        binding.tietTodoTime.setOnClickListener {
            TimePickerDialog(mainActivity,timeListener,myCalendar.get(Calendar.HOUR_OF_DAY),myCalendar.get(Calendar.MINUTE),true).show()
        }

        val dialog = AlertDialog.Builder(mainActivity)
            .setTitle(R.string.label_edit_todo)
            .setView(binding.root)
            .setPositiveButton(R.string.label_update_todo) { _, _ ->


                val todoTitle = binding.tietTodoTitle.text.toString()
                val date = binding.tietTodoDate.text.toString()

                val category = binding.filledExposedDropdown.text.toString()

                todo.todo = todoTitle
                todo.date = date
                todo.category = category
                todo.dateEpoch = myCalendar.timeInMillis
                mainActivity.remote.updateTodo(todo, object : TodoCallback {
                    override fun onResponse(todoCurrent: TodoEntity?, error: String?) {

                        if (error == null) {
                            Toaster(mainActivity).showToast(mainActivity.getString(R.string.update_todo_success_message))
                            val intent = Intent(mainActivity,ReminderBroadcast::class.java)
                            intent.action = Gson().toJson(todo)
                            val pendingIntent = PendingIntent.getBroadcast(mainActivity,Random().nextInt(),intent,
                                PendingIntent.FLAG_CANCEL_CURRENT
                            )
                            val alarmManager = mainActivity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            alarmManager.set(AlarmManager.RTC_WAKEUP,todo.dateEpoch,pendingIntent)
                        } else {
                            Toaster(mainActivity).showToast(error)
                        }
                    }
                })
                mDialog?.dismiss()
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
        binding.filledExposedDropdown.setOnItemClickListener { _, _, position, _ ->
            if(todoAdapter.getCategoryListArray()[position]=="Kategori Ekle ")
            {
                addCategory(false)
              //  dialog.dismiss()
            }

        }



        dialog.show()

        Validator.forceValidation(arrayOf(binding.tietTodoTitle, binding.tietTodoDate , binding.tietTodoTime), dialog)
    }

    // add new categoryItem with custom alert dialog
    private fun addCategory(createAddTodoDialog: Boolean = false) {
        if (mainActivity.currentUserEntity == null) {
            mainActivity.signIn(mainActivity); return
        }

        val binding = DataBindingUtil.inflate<PromptCategoryBinding>(
            layoutInflater, R.layout.prompt_category, null, false
        )
        val colors = mainActivity.resources.getIntArray(R.array.colors)
        binding.tietCategoryColor.setOnClickListener {
            ColorSheet().colorPicker(
                colors = colors,
                listener = { color ->
                    binding.tilCategoryColor.setBackgroundColor(color)
                    binding.tietCategoryColor.setText(" ")
                })
                .show(mainActivity.supportFragmentManager)

        }

        val dialog = AlertDialog.Builder(mainActivity)
            .setTitle(R.string.label_add_category)
            .setView(binding.root)
            .setPositiveButton(R.string.label_add_category) { _, _ ->


                val categoryTitle = binding.tietCategoryTitle.text.toString()
                var colorInt = (binding.tilCategoryColor.background as ColorDrawable).color
                val hexColor = String.format("%06X", 0xFFFFFF and colorInt)
                val category = CategoryEntity(
                    "", categoryTitle, mainActivity.currentUserEntity?.id!!, hexColor
                )

                mainActivity.remote.addCategory(category, object : CategoryCallback {
                    override fun onResponse(category: CategoryEntity?, error: String?) {

                        if (error == null) {
                            Toaster(mainActivity).showToast(mainActivity.getString(R.string.add_category_success_message))
                        /////////////
                            todoAdapter.listOfCategories.add(category!!)
                            if(createAddTodoDialog)
                                addTodo()
                            else
                            {

                                val adapterCategory = ArrayAdapter<String>(
                                    mainActivity,
                                    R.layout.dropdown_menu_popup_item,
                                    todoAdapter.getCategoryListArray()
                                )
                                dialogAddTodo?.findViewById<AutoCompleteTextView>(R.id.filled_exposed_dropdown)?.setAdapter(adapterCategory)
                                dialogAddTodo?.findViewById<AutoCompleteTextView>(R.id.filled_exposed_dropdown)?.setText(category.title,false)
                            }


                        } else {
                            Toaster(mainActivity).showToast(error)
                        }
                    }
                })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> addTodo()}
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
        dialog.show()

        Validator.forceValidation(
            arrayOf(binding.tietCategoryTitle, binding.tietCategoryColor),
            dialog
        )
    }

    private fun editCategory(category: CategoryEntity) {
        if (mainActivity.currentUserEntity == null) {
            mainActivity.signIn(mainActivity); return
        }

        val binding = DataBindingUtil.inflate<PromptCategoryBinding>(
            layoutInflater, R.layout.prompt_category, null, false
        )
        val r = Integer.parseInt(category.color.substring(0, 2), 16)
        val g = Integer.parseInt(category.color.substring(2, 4), 16)
        val b = Integer.parseInt(category.color.substring(4, 6), 16)
        val backgroundColor = Color.rgb(r, g, b)


        binding.tietCategoryTitle.setText(category.title)
        binding.tilCategoryColor.setBackgroundColor(backgroundColor)
        binding.tietCategoryColor.setText(" ")
        val colors = resources.getIntArray(R.array.colors)
        binding.tietCategoryColor.setOnClickListener {
            ColorSheet().colorPicker(
                colors = colors,
                listener = { color ->
                    binding.tilCategoryColor.setBackgroundColor(color)
                    binding.tietCategoryColor.setText(" ")
                })
                .show(mainActivity.supportFragmentManager)

        }
        val dialog = AlertDialog.Builder(mainActivity)
            .setTitle(R.string.label_edit_category)
            .setView(binding.root)
            .setPositiveButton(R.string.label_edit_category) { _, _ ->


                val categoryTitle = binding.tietCategoryTitle.text.toString()
                var colorInt = (binding.tilCategoryColor.background as ColorDrawable).color
                val hexColor =
                    String.format("%06X", 0xFFFFFF and colorInt)

                category.title = categoryTitle
                category.color = hexColor
                mainActivity.remote.updateCategory(category, object : CategoryCallback {
                    override fun onResponse(category: CategoryEntity?, error: String?) {

                        if (error == null) {
                            Toaster(mainActivity).showToast(mainActivity.getString(R.string.update_category_success_message))

                        } else {
                            Toaster(mainActivity).showToast(error)
                        }
                    }
                })
                mDialog?.dismiss()
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
        dialog.show()

        Validator.forceValidation(
            arrayOf(binding.tietCategoryTitle, binding.tietCategoryColor),
            dialog
        )

    }

}