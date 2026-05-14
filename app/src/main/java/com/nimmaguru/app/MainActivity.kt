package com.nimmaguru.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("nimma_guru", MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"
        super.attachBaseContext(newBase.withLanguage(language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NimmaGuruTheme {
                NimmaGuruApp()
            }
        }
    }
}

@Composable
private fun NimmaGuruApp(viewModel: GuruViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AppTab.Directory) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = WarmBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Header(
                onToggleLanguage = {
                    val prefs = context.getSharedPreferences("nimma_guru", Context.MODE_PRIVATE)
                    val current = prefs.getString("language", "en")
                    prefs.edit().putString("language", if (current == "kn") "en" else "kn").apply()
                    (context as? ComponentActivity)?.recreate()
                }
            )
            TabRow(selectedTab = selectedTab, onSelected = { selectedTab = it })
            uiState.errorMessage?.let {
                ErrorBanner(message = it, onDismiss = viewModel::clearError)
            }
            when (selectedTab) {
                AppTab.Directory -> DirectoryScreen(
                    uiState = uiState,
                    onSkillSelected = viewModel::selectSkill,
                    onLocalityChanged = viewModel::updateLocality,
                    onPostAppreciation = viewModel::postAppreciation,
                    onRequestSession = viewModel::requestSession,
                    onSaveProfile = viewModel::saveProfile
                )
                AppTab.Calendar -> CalendarScreen(classes = uiState.classes)
                AppTab.Fame -> FameScreen(gurus = uiState.wallOfFame)
                AppTab.Actions -> StudentActionsScreen(actions = uiState.studentActions)
            }
        }
    }
}

@Composable
private fun Header(onToggleLanguage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Leaf
            )
            Text(
                text = stringResource(R.string.tagline),
                fontSize = 16.sp,
                color = InkMuted
            )
        }
        OutlinedButton(onClick = onToggleLanguage) {
            Text(text = stringResource(R.string.language_toggle), fontSize = 16.sp)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TabRow(selectedTab: AppTab, onSelected: (AppTab) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            if (selected) {
                Button(onClick = { onSelected(tab) }) {
                    Text(tab.label, fontSize = 15.sp)
                }
            } else {
                OutlinedButton(onClick = { onSelected(tab) }) {
                    Text(tab.label, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DirectoryScreen(
    uiState: GuruUiState,
    onSkillSelected: (String?) -> Unit,
    onLocalityChanged: (String) -> Unit,
    onPostAppreciation: (Guru, String, String) -> Unit,
    onRequestSession: (Guru, String) -> Unit,
    onSaveProfile: (Guru, List<String>, String, String) -> Unit
) {
    val skills = listOf("Math", "Science", "Physics", "Carpentry", "English", "Kannada")

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SectionTitle(text = stringResource(R.string.directory))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skills.forEach { skill ->
                    val selected = uiState.selectedSkill == skill
                    if (selected) {
                        Button(onClick = { onSkillSelected(null) }) { Text(skill, fontSize = 16.sp) }
                    } else {
                        OutlinedButton(onClick = { onSkillSelected(skill) }) { Text(skill, fontSize = 16.sp) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = uiState.localityQuery,
                onValueChange = onLocalityChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_hint), fontSize = 16.sp) },
                singleLine = true
            )
        }

        if (uiState.isLoading) {
            item { BodyText("Connecting to Firebase...") }
        } else if (uiState.filteredGurus.isEmpty()) {
            item { BodyText(stringResource(R.string.no_results)) }
        } else {
            items(uiState.filteredGurus, key = { it.id }) { guru ->
                GuruCard(
                    guru = guru,
                    onPostAppreciation = onPostAppreciation,
                    onRequestSession = onRequestSession,
                    onSaveProfile = onSaveProfile
                )
            }
        }
    }
}

@Composable
private fun GuruCard(
    guru: Guru,
    onPostAppreciation: (Guru, String, String) -> Unit,
    onRequestSession: (Guru, String) -> Unit,
    onSaveProfile: (Guru, List<String>, String, String) -> Unit
) {
    var appreciationOpen by remember { mutableStateOf(false) }
    var requestOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }

    AppCard {
        Text(guru.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        BodyText(guru.skills.joinToString("  |  "))
        BodyText("${guru.locality}  |  ${guru.languages.joinToString(", ")}")
        BodyText(guru.bio)
        BodyText("${stringResource(R.string.available_hours)}: ${guru.freeHours}")
        BodyText("${stringResource(R.string.contact)}: ${guru.contact}")
        Text(
            text = "${stringResource(R.string.appreciation_wall)} (${guru.appreciationCount})",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Leaf,
            modifier = Modifier.padding(top = 8.dp)
        )
        guru.appreciations.takeLast(3).reversed().forEach {
            BodyText("\"${it.message}\" - ${it.studentName}, ${it.createdAt}")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = { requestOpen = true }) {
                Text("Request", fontSize = 15.sp)
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { appreciationOpen = true }) {
                Text(stringResource(R.string.post_appreciation), fontSize = 15.sp)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { editOpen = true }) {
                Text(stringResource(R.string.edit_profile), fontSize = 15.sp)
            }
        }
    }

    if (appreciationOpen) {
        AppreciationDialog(
            guru = guru,
            onDismiss = { appreciationOpen = false },
            onSubmit = { student, message ->
                onPostAppreciation(guru, student, message)
                appreciationOpen = false
            }
        )
    }
    if (requestOpen) {
        RequestSessionDialog(
            guru = guru,
            onDismiss = { requestOpen = false },
            onSubmit = { student ->
                onRequestSession(guru, student)
                requestOpen = false
            }
        )
    }
    if (editOpen) {
        EditProfileDialog(
            guru = guru,
            onDismiss = { editOpen = false },
            onSubmit = { skills, freeHours, contact ->
                onSaveProfile(guru, skills, freeHours, contact)
                editOpen = false
            }
        )
    }
}

@Composable
private fun AppreciationDialog(guru: Guru, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var studentName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(guru.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(studentName, { studentName = it }, label = { Text("Student name") })
                OutlinedTextField(message, { message = it }, label = { Text("Thank you note") }, minLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(studentName, message) }) {
                Text(stringResource(R.string.post_appreciation))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RequestSessionDialog(guru: Guru, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var studentName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BodyText("${guru.name} is available: ${guru.freeHours}")
                OutlinedTextField(studentName, { studentName = it }, label = { Text("Student name") })
            }
        },
        confirmButton = { Button(onClick = { onSubmit(studentName) }) { Text("Request") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditProfileDialog(
    guru: Guru,
    onDismiss: () -> Unit,
    onSubmit: (List<String>, String, String) -> Unit
) {
    var skills by remember { mutableStateOf(guru.skills.joinToString(", ")) }
    var freeHours by remember { mutableStateOf(guru.freeHours) }
    var contact by remember { mutableStateOf(guru.contact) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_profile)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(skills, { skills = it }, label = { Text("Skills, comma separated") })
                OutlinedTextField(freeHours, { freeHours = it }, label = { Text(stringResource(R.string.available_hours)) })
                OutlinedTextField(contact, { contact = it }, label = { Text(stringResource(R.string.contact)) })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        skills.split(",").map { it.trim() }.filter { it.isNotBlank() },
                        freeHours,
                        contact
                    )
                }
            ) {
                Text(stringResource(R.string.save_profile))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CalendarScreen(classes: List<GuruClass>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle(stringResource(R.string.class_calendar)) }
        items(classes) { item ->
            AppCard {
                Text(item.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                BodyText("${item.skill} with ${item.guruName}")
                BodyText("${item.dateTime}  |  ${item.venue}")
                BodyText(item.locality)
            }
        }
    }
}

@Composable
private fun FameScreen(gurus: List<Guru>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle(stringResource(R.string.wall_of_fame)) }
        items(gurus) { guru ->
            AppCard {
                Text(guru.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                BodyText("${guru.appreciationCount} appreciations")
                BodyText(guru.skills.joinToString("  |  "))
                BodyText(guru.locality)
            }
        }
    }
}

@Composable
private fun StudentActionsScreen(actions: List<StudentAction>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Student Actions") }
        if (actions.isEmpty()) {
            item { BodyText("No student actions yet.") }
        } else {
            items(actions, key = { it.id }) { action ->
                AppCard {
                    Text(action.action, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                    BodyText("${action.studentName} -> ${action.guruName}")
                    BodyText(action.message)
                    BodyText(action.createdAt)
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE9E4), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, modifier = Modifier.weight(1f), fontSize = 16.sp, color = Color(0xFF7A2418))
        TextButton(onClick = onDismiss) { Text("OK") }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun AppCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Leaf,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun BodyText(text: String) {
    Text(text = text, fontSize = 16.sp, color = InkMuted)
}

@Composable
private fun NimmaGuruTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Leaf,
            secondary = Gold,
            background = WarmBackground,
            surface = Color.White
        ),
        content = content
    )
}

private enum class AppTab(val label: String) {
    Directory("Directory"),
    Calendar("Calendar"),
    Fame("Fame"),
    Actions("Actions")
}

private val WarmBackground = Color(0xFFF7FBF4)
private val Leaf = Color(0xFF1A6E3A)
private val Gold = Color(0xFFF1B84B)
private val Ink = Color(0xFF1D3726)
private val InkMuted = Color(0xFF3E4B41)

private fun Context.withLanguage(language: String): Context {
    val locale = Locale(language)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}
