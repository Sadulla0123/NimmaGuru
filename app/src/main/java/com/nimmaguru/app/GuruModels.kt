package com.nimmaguru.app

data class Guru(
    val id: String,
    val name: String,
    val skills: List<String>,
    val languages: List<String>,
    val locality: String,
    val bio: String,
    val freeHours: String,
    val contact: String,
    val appreciations: MutableList<Appreciation>
) {
    val appreciationCount: Int
        get() = appreciations.size
}

data class Appreciation(
    val studentName: String,
    val message: String,
    val createdAt: String
)

data class GuruClass(
    val title: String,
    val guruName: String,
    val skill: String,
    val dateTime: String,
    val venue: String,
    val locality: String
)

data class StudentAction(
    val id: String,
    val guruId: String,
    val guruName: String,
    val studentName: String,
    val action: String,
    val message: String,
    val createdAt: String
)

data class GuruUiState(
    val gurus: List<Guru> = emptyList(),
    val classes: List<GuruClass> = emptyList(),
    val studentActions: List<StudentAction> = emptyList(),
    val selectedSkill: String? = null,
    val localityQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val filteredGurus: List<Guru>
        get() {
            val locality = localityQuery.trim().lowercase()
            return gurus.filter { guru ->
                val skillMatch = selectedSkill == null || guru.skills.any {
                    it.equals(selectedSkill, ignoreCase = true)
                }
                val localityMatch = locality.isBlank() || guru.locality.lowercase().contains(locality)
                skillMatch && localityMatch
            }
        }

    val wallOfFame: List<Guru>
        get() = gurus.sortedByDescending { it.appreciationCount }
}
