package com.nimmaguru.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GuruRepository {
    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var guruListener: ListenerRegistration? = null
    private var classListener: ListenerRegistration? = null
    private var actionListener: ListenerRegistration? = null

    fun connect(
        onGurusChanged: (List<Guru>) -> Unit,
        onClassesChanged: (List<GuruClass>) -> Unit,
        onStudentActionsChanged: (List<StudentAction>) -> Unit,
        onError: (String) -> Unit
    ) {
        val db = try {
            FirebaseFirestore.getInstance().also { firestore = it }
        } catch (exception: IllegalStateException) {
            onError("Firebase is not configured. Add app/google-services.json and sync the project.")
            return
        }
        auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()

        signInStudent(onError)
        seedDefaultsIfNeeded(db, onError)

        guruListener = db.collection(GURUS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Unable to load Gurus")
                    return@addSnapshotListener
                }
                onGurusChanged(snapshot?.documents.orEmpty().map { document ->
                    Guru(
                        id = document.id,
                        name = document.getString("name").orEmpty(),
                        skills = document.stringList("skills"),
                        languages = document.stringList("languages"),
                        locality = document.getString("locality").orEmpty(),
                        bio = document.getString("bio").orEmpty(),
                        freeHours = document.getString("freeHours").orEmpty(),
                        contact = document.getString("contact").orEmpty(),
                        appreciations = document.appreciationMaps().toMutableList()
                    )
                })
            }

        classListener = db.collection(CLASSES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Unable to load classes")
                    return@addSnapshotListener
                }
                onClassesChanged(snapshot?.documents.orEmpty().map { document ->
                    GuruClass(
                        title = document.getString("title").orEmpty(),
                        guruName = document.getString("guruName").orEmpty(),
                        skill = document.getString("skill").orEmpty(),
                        dateTime = document.getString("dateTime").orEmpty(),
                        venue = document.getString("venue").orEmpty(),
                        locality = document.getString("locality").orEmpty()
                    )
                })
            }

        actionListener = db.collection(STUDENT_ACTIONS)
            .orderBy("sortTime", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Unable to load student actions")
                    return@addSnapshotListener
                }
                onStudentActionsChanged(snapshot?.documents.orEmpty().map { document ->
                    StudentAction(
                        id = document.id,
                        guruId = document.getString("guruId").orEmpty(),
                        guruName = document.getString("guruName").orEmpty(),
                        studentName = document.getString("studentName").orEmpty(),
                        action = document.getString("action").orEmpty(),
                        message = document.getString("message").orEmpty(),
                        createdAt = document.getString("createdAt").orEmpty()
                    )
                })
            }
    }

    fun postAppreciation(guru: Guru, studentName: String, message: String, onError: (String) -> Unit) {
        val db = firestore ?: return onError("Firebase is not connected yet.")
        val appreciation = mapOf(
            "studentName" to studentName.ifBlank { "Student" },
            "message" to message.ifBlank { "Thank you for your guidance." },
            "createdAt" to nowLabel()
        )
        db.collection(GURUS).document(guru.id)
            .update("appreciations", FieldValue.arrayUnion(appreciation))
            .addOnFailureListener { onError(it.localizedMessage ?: "Unable to post appreciation") }

        recordStudentAction(
            guru = guru,
            studentName = appreciation["studentName"].orEmpty(),
            action = "Posted appreciation",
            message = appreciation["message"].orEmpty(),
            onError = onError
        )
    }

    fun requestSession(guru: Guru, studentName: String, onError: (String) -> Unit) {
        recordStudentAction(
            guru = guru,
            studentName = studentName.ifBlank { "Student" },
            action = "Requested session",
            message = "Asked to join ${guru.freeHours}",
            onError = onError
        )
    }

    fun updateGuru(guru: Guru, skills: List<String>, freeHours: String, contact: String, onError: (String) -> Unit) {
        val db = firestore ?: return onError("Firebase is not connected yet.")
        db.collection(GURUS).document(guru.id)
            .update(
                mapOf(
                    "skills" to skills,
                    "freeHours" to freeHours,
                    "contact" to contact
                )
            )
            .addOnFailureListener { onError(it.localizedMessage ?: "Unable to update profile") }
    }

    fun close() {
        guruListener?.remove()
        classListener?.remove()
        actionListener?.remove()
    }

    private fun signInStudent(onError: (String) -> Unit) {
        val firebaseAuth = auth ?: return
        if (firebaseAuth.currentUser != null) return
        firebaseAuth.signInAnonymously()
            .addOnFailureListener { onError(it.localizedMessage ?: "Anonymous student sign-in failed") }
    }

    private fun recordStudentAction(
        guru: Guru,
        studentName: String,
        action: String,
        message: String,
        onError: (String) -> Unit
    ) {
        val db = firestore ?: return onError("Firebase is not connected yet.")
        db.collection(STUDENT_ACTIONS).add(
            mapOf(
                "guruId" to guru.id,
                "guruName" to guru.name,
                "studentName" to studentName,
                "action" to action,
                "message" to message,
                "createdAt" to nowLabel(),
                "sortTime" to System.currentTimeMillis()
            )
        ).addOnFailureListener { onError(it.localizedMessage ?: "Unable to save student action") }
    }

    private fun seedDefaultsIfNeeded(db: FirebaseFirestore, onError: (String) -> Unit) {
        db.collection(GURUS).limit(1).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) return@addOnSuccessListener
                defaultGurus().forEach { guru ->
                    db.collection(GURUS).document(guru.id).set(guru.toFirestoreMap())
                }
                defaultClasses().forEach { guruClass ->
                    db.collection(CLASSES).add(guruClass.toFirestoreMap())
                }
            }
            .addOnFailureListener { onError(it.localizedMessage ?: "Unable to seed Firebase data") }
    }

    private fun nowLabel(): String =
        SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date())

    private fun defaultGurus(): List<Guru> = listOf(
        Guru(
            id = "g1",
            name = "Savitri Rao",
            skills = listOf("Math", "Science"),
            languages = listOf("Kannada", "English"),
            locality = "Basavanagudi",
            bio = "Retired high-school teacher helping students build strong basics.",
            freeHours = "Mon, Wed, Fri - 5:00 PM to 7:00 PM",
            contact = "Community centre desk",
            appreciations = mutableListOf(
                Appreciation("Asha", "Thank you for making fractions easy.", "Today"),
                Appreciation("Ravi", "Your science examples helped my exam prep.", "Yesterday")
            )
        ),
        Guru(
            id = "g2",
            name = "Krishna Murthy",
            skills = listOf("Carpentry", "Physics"),
            languages = listOf("Kannada"),
            locality = "Mysuru Road",
            bio = "Retired engineer and craftsperson teaching practical making skills.",
            freeHours = "Tue, Thu - 4:30 PM to 6:30 PM",
            contact = "Samudaya Bhavana notice board",
            appreciations = mutableListOf(
                Appreciation("Meena", "I built my first wooden shelf with your guidance.", "Mon")
            )
        ),
        Guru(
            id = "g3",
            name = "Fathima Begum",
            skills = listOf("English", "Kannada"),
            languages = listOf("Kannada", "English", "Urdu"),
            locality = "Kengeri",
            bio = "Former librarian supporting reading, speaking, and homework help.",
            freeHours = "Sat, Sun - 10:00 AM to 12:00 PM",
            contact = "Local library volunteer desk",
            appreciations = mutableListOf(
                Appreciation("Nikhil", "Your reading club made me confident.", "Fri"),
                Appreciation("Sana", "Thank you for correcting my essay patiently.", "Thu"),
                Appreciation("Deepa", "The story sessions are wonderful.", "Wed")
            )
        )
    )

    private fun defaultClasses(): List<GuruClass> = listOf(
        GuruClass("Math basics circle", "Savitri Rao", "Math", "15 May, 5:00 PM", "Samudaya Bhavana Hall 1", "Basavanagudi"),
        GuruClass("Hands-on carpentry", "Krishna Murthy", "Carpentry", "16 May, 4:30 PM", "Community workshop", "Mysuru Road"),
        GuruClass("Reading confidence club", "Fathima Begum", "English", "18 May, 10:00 AM", "Local library", "Kengeri")
    )

    private companion object {
        const val GURUS = "gurus"
        const val CLASSES = "classes"
        const val STUDENT_ACTIONS = "student_actions"
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.appreciationMaps(): List<Appreciation> {
    val raw = get("appreciations") as? List<*> ?: return emptyList()
    return raw.mapNotNull { item ->
        val map = item as? Map<*, *> ?: return@mapNotNull null
        Appreciation(
            studentName = map["studentName"] as? String ?: "Student",
            message = map["message"] as? String ?: "",
            createdAt = map["createdAt"] as? String ?: ""
        )
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.stringList(field: String): List<String> {
    val raw = get(field) as? List<*> ?: return emptyList()
    return raw.filterIsInstance<String>()
}

private fun Guru.toFirestoreMap(): Map<String, Any> = mapOf(
    "name" to name,
    "skills" to skills,
    "languages" to languages,
    "locality" to locality,
    "bio" to bio,
    "freeHours" to freeHours,
    "contact" to contact,
    "appreciations" to appreciations.map {
        mapOf("studentName" to it.studentName, "message" to it.message, "createdAt" to it.createdAt)
    }
)

private fun GuruClass.toFirestoreMap(): Map<String, Any> = mapOf(
    "title" to title,
    "guruName" to guruName,
    "skill" to skill,
    "dateTime" to dateTime,
    "venue" to venue,
    "locality" to locality
)
