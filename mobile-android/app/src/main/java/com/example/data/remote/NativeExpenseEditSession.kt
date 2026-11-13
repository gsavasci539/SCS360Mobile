package com.example.data.remote

import com.example.data.model.Expense
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ExpenseEditDraft(val title:String,val category:String,val amount:String,val date:String,val notes:String?)
data class ExpenseEditSnapshot(val expense:Expense,val instant:String) {
    fun draft()=ExpenseEditDraft(expense.title,expense.category,NativeExpenseClient.money(expense.amount.toString()).setScale(2).toPlainString(),expense.date,expense.notes)
}

/** Canonical delta PUT; repeated delivery updates the same record and never creates a ledger row. */
class NativeExpenseEditSession(expenseId:String,organizationId:String) {
    private val api=NativeExpenseClient(); private val id=UUID.fromString(expenseId).toString()
    private val organization=UUID.fromString(organizationId).toString(); private val mutex=Mutex()
    private var original:ExpenseEditSnapshot?=null; private var pending:String?=null; private var acknowledged:ExpenseEditSnapshot?=null
    val hasPending get()=pending!=null && acknowledged==null
    fun current()=api.current()
    private fun instant(value:String)=OffsetDateTime.parse(if(value.matches(Regex(".*(?:Z|[+-]\\d{2}:\\d{2})$"))) value else value+"Z").toInstant()
    private fun parse(row:JSONObject):ExpenseEditSnapshot {
        check(UUID.fromString(row.getString("organization_id")).toString()==organization) { "Gider kurumu eşleşmedi" }
        val expense=api.parse(row); check(expense.id==id) { "Gider kimliği eşleşmedi" }
        val date=row.getString("expense_date"); instant(date)
        return ExpenseEditSnapshot(expense,date)
    }
    suspend fun load():ExpenseEditSnapshot=mutex.withLock {
        val result=parse(api.request("finance/expenses/$id")); api.ensureCurrent()
        original=result; pending=null; acknowledged=null; result
    }
    suspend fun save(draft:ExpenseEditDraft):ExpenseEditSnapshot=mutex.withLock {
        api.ensureCurrent(); acknowledged?.let { return@withLock it }
        val before=checkNotNull(original) { "Önce güncel gideri okuyun" }; val old=before.draft()
        val title=draft.title.trim(); val category=draft.category.trim(); val amount=NativeExpenseClient.money(draft.amount).setScale(2).toPlainString()
        val day=LocalDate.parse(draft.date).toString()
        require(title.isNotEmpty() && title.length<=500 && category.isNotEmpty() && category.length<=100)
        val notes=if(draft.notes==old.notes) old.notes else draft.notes?.takeIf { it.isNotBlank() }
        require(notes==old.notes || notes==null || notes.length<=2000)
        val patch=JSONObject()
        if(title!=old.title) patch.put("description",title)
        if(category!=old.category) patch.put("category",category)
        if(amount!=old.amount) patch.put("amount",amount)
        if(day!=old.date) patch.put("expense_date",day+"T00:00:00+03:00")
        if(notes!=old.notes) patch.put("notes",notes ?: JSONObject.NULL)
        require(patch.length()>0) { "Kaydedilecek değişiklik yok" }
        val body=patch.toString(); check(pending==null || pending==body) { "Bekleyen değişikliği doğrulayın veya sunucudan yeniden okuyun" }; pending=body
        try {
            val result=parse(api.request("finance/expenses/$id",body,put=true)); val expense=result.expense
            check(expense.branch_id==before.expense.branch_id && expense.created_by==before.expense.created_by)
            for(field in patch.keys()) when(field) {
                "description" -> check(expense.title==title)
                "category" -> check(expense.category==category)
                "amount" -> check(NativeExpenseClient.money(expense.amount.toString()).compareTo(amount.toBigDecimal())==0)
                "expense_date" -> check(instant(result.instant)==instant(patch.getString(field)) && expense.date==day)
                "notes" -> check(expense.notes==notes)
            }
            api.ensureCurrent(); acknowledged=result; result
        } catch(failure:ExpenseHttpException) {
            if(failure.code==422) pending=null
            throw failure
        }
    }
}
