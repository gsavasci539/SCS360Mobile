package com.example.data.local

import com.example.data.model.AttendanceRecord
import com.example.data.model.Invoice
import com.example.data.model.Student

// ---------------------------------------------------------
// Student Entity <-> Domain Model Mappings
// ---------------------------------------------------------

fun StudentEntity.toStudent(): Student {
    return Student(
        id = id,
        student_number = student_number,
        first_name = first_name,
        last_name = last_name,
        class_id = class_id,
        class_name = class_name,
        branch_id = branch_id,
        parent_name = parent_name,
        parent_phone = parent_phone,
        parent_email = parent_email,
        status = status,
        allergies_or_diet = allergies_or_diet,
        blood_type = blood_type ?: "A Rh+",
        avatar_color_hex = avatar_color_hex,
        tuition_balance = tuition_balance,
        today_attendance_status = today_attendance_status
    )
}

fun Student.toEntity(): StudentEntity {
    return StudentEntity(
        id = id,
        student_number = student_number,
        first_name = first_name,
        last_name = last_name,
        class_id = class_id,
        class_name = class_name,
        branch_id = branch_id,
        parent_name = parent_name,
        parent_phone = parent_phone,
        parent_email = parent_email,
        status = status,
        allergies_or_diet = allergies_or_diet,
        blood_type = blood_type,
        avatar_color_hex = avatar_color_hex,
        tuition_balance = tuition_balance,
        today_attendance_status = today_attendance_status
    )
}

// ---------------------------------------------------------
// Attendance Entity <-> Domain Model Mappings
// ---------------------------------------------------------

fun AttendanceEntity.toAttendanceRecord(): AttendanceRecord {
    return AttendanceRecord(
        id = id,
        student_id = student_id,
        student_name = student_name,
        class_id = class_id,
        date = date,
        status = status,
        note = note,
        check_in_time = check_in_time,
        temperature_celsius = temperature_celsius,
        parent_notified = is_synced
    )
}

fun AttendanceRecord.toEntity(isSynced: Boolean = true): AttendanceEntity {
    return AttendanceEntity(
        id = id,
        student_id = student_id,
        student_name = student_name,
        class_id = class_id,
        date = date,
        status = status,
        note = note,
        check_in_time = check_in_time,
        temperature_celsius = temperature_celsius,
        is_synced = isSynced
    )
}

// ---------------------------------------------------------
// Finance / Invoice Entity <-> Domain Model Mappings
// ---------------------------------------------------------

fun InvoiceEntity.toInvoice(): Invoice {
    return Invoice(
        id = id,
        invoice_number = invoice_number,
        student_id = student_id,
        student_name = student_name,
        class_name = class_name,
        amount = amount,
        paid_amount = paid_amount,
        due_date = due_date,
        issue_date = issue_date,
        status = status,
        description = description
    )
}

fun Invoice.toEntity(): InvoiceEntity {
    return InvoiceEntity(
        id = id,
        invoice_number = invoice_number,
        student_id = student_id,
        student_name = student_name,
        class_name = class_name,
        amount = amount,
        paid_amount = paid_amount,
        due_date = due_date,
        issue_date = issue_date,
        status = status,
        description = description
    )
}
