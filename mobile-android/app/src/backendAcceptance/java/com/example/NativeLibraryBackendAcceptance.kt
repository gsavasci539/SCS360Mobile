package com.example

import com.example.data.model.*
import com.example.data.remote.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36],manifest=Config.NONE)
class NativeLibraryBackendAcceptance {
    private fun env(name:String)=System.getenv(name)?:error("Missing isolated input: $name")
    @Test fun actualNativeLoginCatalogBorrowReturnAndReadOnlyScope()=runBlocking {
        val base=ApiClientProvider.activeBaseUrl;val interceptor=ApiClientProvider.interceptor;val token=interceptor.authToken
        try {
            val auth=NativeAuthentication(ApiClientProvider.createService(env("NATIVE_TEST_API_URL")))
            val writer=auth.login("native-library@example.com","Synthetic-pass-2026!");assertEquals(UserRole.CUSTOM,SessionRolePolicy.primary(writer))
            val writerToken=interceptor.authToken!!
            val api=NativeLibraryClient();assertTrue(api.books().isEmpty())
            val book=api.createBook(JSONObject().put("title","Native recorded catalog").put("author","Recorded author").put("category","Acceptance").put("barcode","NATIVE-ACCEPTANCE").put("total_copies",2))
            assertEquals(book.id,api.books().single().id);assertEquals("",book.publisher)
            val detail=api.book(book.id);assertEquals("NATIVE-ACCEPTANCE",detail.barcode);assertTrue(detail.updated_at.isNotBlank())
            val edited=api.updateBook(book.id,detail.updated_at,JSONObject().put("title","Native edited catalog").put("publisher","Acceptance Press").put("location","A-2"))
            assertEquals("Native edited catalog",edited.title);assertEquals("Acceptance Press",edited.publisher);assertEquals(book.id,api.books().single().id)
            try {api.updateBook(book.id,detail.updated_at,JSONObject().put("title","Stale write"));fail("Stale catalog edit")}
            catch(failure:RegistryHttpException){assertEquals(409,failure.code)}
            assertEquals("Native edited catalog",api.book(book.id).title)
            val removable=api.createBook(JSONObject().put("title","Native removable catalog").put("author","Recorded author").put("category","Acceptance").put("total_copies",1))
            api.deleteBook(removable.id);assertTrue(api.books().none{it.id==removable.id})
            try {api.book(removable.id);fail("Deleted catalog detail")}
            catch(failure:RegistryHttpException){assertEquals(404,failure.code)}
            val student=NativeRegistryClient().students(null,null,null).single{it.id==env("NATIVE_TEST_STUDENT")}
            val saved=api.borrow(book.id,student.copy(first_name="Untrusted",last_name="Name"),14)
            assertEquals(student.full_name,saved.student_name);assertEquals(1,api.books().single().available_copies)
            try {api.borrow(book.id,Student(id=env("NATIVE_TEST_FOREIGN_STUDENT")),14);fail("Foreign student")}catch(failure:RegistryHttpException){assertTrue(failure.code in listOf(403,404))}
            assertEquals(1,api.books().single().available_copies)
            auth.login("native-library-reader@example.com","Synthetic-pass-2026!")
            val reader=NativeLibraryClient();assertEquals(book.id,reader.books().single().id)
            assertEquals("Native edited catalog",reader.book(book.id).title)
            try {reader.borrow(book.id,student,14);fail("Reader write")}catch(failure:RegistryHttpException){assertEquals(403,failure.code)}
            try {reader.updateBook(book.id,reader.book(book.id).updated_at,JSONObject().put("title","Reader write"));fail("Reader edit")}
            catch(failure:RegistryHttpException){assertEquals(403,failure.code)}
            try {reader.deleteBook(book.id);fail("Reader delete")}catch(failure:RegistryHttpException){assertEquals(403,failure.code)}
            auth.restore(writerToken)
            val restored=NativeLibraryClient();assertTrue(restored.returnBook(saved.id).is_returned);assertEquals(2,restored.books().single().available_copies)
            try {restored.returnBook(saved.id);fail("Duplicate return")}catch(failure:RegistryHttpException){assertEquals(409,failure.code)}
            try {restored.deleteBook(book.id);fail("Book with loan history")}
            catch(failure:RegistryHttpException){assertEquals(409,failure.code)}
            assertEquals(1,restored.loans().size);assertEquals("",saved.parent_phone)
        }finally{ApiClientProvider.createService(base);interceptor.setSession(token)}
    }
}
