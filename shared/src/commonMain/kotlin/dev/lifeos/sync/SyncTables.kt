package dev.lifeos.sync

/** Tablas que se sincronizan y sus columnas (igual que en los .sq y en supabase/migrations). */
internal val SYNC_TABLES: List<SyncTable> = listOf(
    SyncTable("project", listOf("id" to Col.TEXT, "name" to Col.TEXT, "kind" to Col.TEXT, "description" to Col.TEXT, "due_date" to Col.TEXT, "sort_order" to Col.INT, "created_at" to Col.INT, "updated_at" to Col.INT, "deleted_at" to Col.INT)),
    SyncTable("milestone", listOf("id" to Col.TEXT, "project_id" to Col.TEXT, "title" to Col.TEXT, "deliverable" to Col.TEXT, "due_date" to Col.TEXT, "created_at" to Col.INT, "updated_at" to Col.INT, "deleted_at" to Col.INT)),
    SyncTable("task", listOf("id" to Col.TEXT, "project_id" to Col.TEXT, "milestone_id" to Col.TEXT, "title" to Col.TEXT, "notes" to Col.TEXT, "due_date" to Col.TEXT, "planned_date" to Col.TEXT, "estimate_blocks" to Col.INT, "done_at" to Col.INT, "created_at" to Col.INT, "updated_at" to Col.INT, "deleted_at" to Col.INT)),
    SyncTable("daily_plan", listOf("id" to Col.TEXT, "first_task_id" to Col.TEXT, "chosen_at" to Col.INT, "created_at" to Col.INT, "updated_at" to Col.INT, "deleted_at" to Col.INT)),
    SyncTable("habit", listOf("id" to Col.TEXT, "name" to Col.TEXT, "period" to Col.TEXT, "target" to Col.INT, "target_max" to Col.INT, "sort_order" to Col.INT, "created_at" to Col.INT, "updated_at" to Col.INT, "deleted_at" to Col.INT)),
    SyncTable("habit_log", listOf("id" to Col.TEXT, "habit_id" to Col.TEXT, "date" to Col.TEXT, "logged_at" to Col.INT, "created_at" to Col.INT, "updated_at" to Col.INT, "deleted_at" to Col.INT)),
    SyncTable("deadline", listOf("id" to Col.TEXT, "project_id" to Col.TEXT, "title" to Col.TEXT, "kind" to Col.TEXT, "due_date" to Col.TEXT, "done_at" to Col.INT, "created_at" to Col.INT, "updated_at" to Col.INT, "deleted_at" to Col.INT)),
    SyncTable("inbox_item", listOf("id" to Col.TEXT, "text" to Col.TEXT, "source" to Col.TEXT, "created_at" to Col.INT, "updated_at" to Col.INT, "deleted_at" to Col.INT)),
    SyncTable("note", listOf("id" to Col.TEXT, "project_id" to Col.TEXT, "kind" to Col.TEXT, "body" to Col.TEXT, "url" to Col.TEXT, "created_at" to Col.INT, "updated_at" to Col.INT, "deleted_at" to Col.INT)),
    SyncTable("event", listOf("id" to Col.TEXT, "project_id" to Col.TEXT, "title" to Col.TEXT, "kind" to Col.TEXT, "date" to Col.TEXT, "start_min" to Col.INT, "end_min" to Col.INT, "repeat_weekly" to Col.INT, "until_date" to Col.TEXT, "skip_dates" to Col.TEXT, "location" to Col.TEXT, "created_at" to Col.INT, "updated_at" to Col.INT, "deleted_at" to Col.INT)),
    SyncTable("focus_session", listOf("id" to Col.TEXT, "task_id" to Col.TEXT, "project_id" to Col.TEXT, "title" to Col.TEXT, "started_at" to Col.INT, "planned_ms" to Col.INT, "paused_ms" to Col.INT, "paused_at" to Col.INT, "ended_at" to Col.INT, "block_index" to Col.INT, "block_total" to Col.INT, "created_at" to Col.INT, "updated_at" to Col.INT, "deleted_at" to Col.INT)),
)

enum class Col { TEXT, INT }

class SyncTable(val name: String, val columns: List<Pair<String, Col>>) {
    val names: List<String> = columns.map { it.first }
}
