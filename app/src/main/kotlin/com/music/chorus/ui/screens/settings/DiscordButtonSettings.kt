package com.music.chorus.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.ui.component.TopBarBackButton
import androidx.navigation.NavController
import pushkar.chorus.music.R
import pushkar.chorus.music.constants.DiscordActivityButton1CustomUrlKey
import pushkar.chorus.music.constants.DiscordActivityButton1EnabledKey
import pushkar.chorus.music.constants.DiscordActivityButton1LabelKey
import pushkar.chorus.music.constants.DiscordActivityButton1UrlSourceKey
import pushkar.chorus.music.constants.DiscordActivityButton2CustomUrlKey
import pushkar.chorus.music.constants.DiscordActivityButton2EnabledKey
import pushkar.chorus.music.constants.DiscordActivityButton2LabelKey
import pushkar.chorus.music.constants.DiscordActivityButton2UrlSourceKey
import pushkar.chorus.music.ui.component.EditTextPreference
import pushkar.chorus.music.ui.component.ListPreference
import pushkar.chorus.music.ui.component.SwitchPreference
import pushkar.chorus.music.utils.rememberPreference

private val DiscordButtonUrlOptions =
    listOf("songurl", "artisturl", "albumurl", "custom")

@Composable
fun DiscordButtonSettings(navController: NavController) {
    val context = LocalContext.current

    val (button1Label, onButton1LabelChange) =
        rememberPreference(
            key = DiscordActivityButton1LabelKey,
            defaultValue = "Listen on Chorus Music",
        )
    val (button1Enabled, onButton1EnabledChange) =
        rememberPreference(
            key = DiscordActivityButton1EnabledKey,
            defaultValue = true,
        )
    val (button2Label, onButton2LabelChange) =
        rememberPreference(
            key = DiscordActivityButton2LabelKey,
            defaultValue = "Go to Chorus Music",
        )
    val (button2Enabled, onButton2EnabledChange) =
        rememberPreference(
            key = DiscordActivityButton2EnabledKey,
            defaultValue = false,
        )

    val (button1UrlSource, onButton1UrlSourceChange) =
        rememberPreference(
            key = DiscordActivityButton1UrlSourceKey,
            defaultValue = "songurl",
        )
    val (button1CustomUrl, onButton1CustomUrlChange) =
        rememberPreference(
            key = DiscordActivityButton1CustomUrlKey,
            defaultValue = "",
        )
    val (button2UrlSource, onButton2UrlSourceChange) =
        rememberPreference(
            key = DiscordActivityButton2UrlSourceKey,
            defaultValue = "custom",
        )
    val (button2CustomUrl, onButton2CustomUrlChange) =
        rememberPreference(
            key = DiscordActivityButton2CustomUrlKey,
            defaultValue = "https://chorus-mu.github.io/chorusweb",
        )

    Scaffold { inner ->
        Column(Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.discord_button_settings),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    TopBarBackButton(navController = navController)
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        bottom = inner.calculateBottomPadding() + 80.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Discord Button Options",
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        SwitchPreference(
                            title = { Text("Show Button 1") },
                            description = "Show Button 1 on Discord RPC",
                            icon = { Icon(painterResource(R.drawable.add), null) },
                            checked = button1Enabled,
                            onCheckedChange = onButton1EnabledChange,
                        )

                        if (button1Enabled) {
                            EditTextPreference(
                                title = { Text("Button 1 Label") },
                                icon = { Icon(painterResource(R.drawable.edit), null) },
                                value = button1Label,
                                onValueChange = onButton1LabelChange,
                                isInputValid = { true },
                            )

                            ListPreference(
                                title = { Text("Button 1 URL Source") },
                                icon = { Icon(painterResource(R.drawable.link), null) },
                                selectedValue = button1UrlSource,
                                values = DiscordButtonUrlOptions,
                                valueText = { discordUrlSourceLabel(it) },
                                onValueSelected = onButton1UrlSourceChange,
                            )
                        }

                        if (button1Enabled && button1UrlSource == "custom") {
                            EditTextPreference(
                                title = { Text("Button 1 Custom URL") },
                                icon = { Icon(painterResource(R.drawable.link), null) },
                                value = button1CustomUrl,
                                onValueChange = onButton1CustomUrlChange,
                                isInputValid = { true },
                            )
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Discord Button 2 Options",
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        SwitchPreference(
                            title = { Text("Show Button 2") },
                            description = "Show Button 2 on Discord RPC",
                            icon = { Icon(painterResource(R.drawable.add), null) },
                            checked = button2Enabled,
                            onCheckedChange = onButton2EnabledChange,
                        )

                        if (button2Enabled) {
                            EditTextPreference(
                                title = { Text("Button 2 Label") },
                                icon = { Icon(painterResource(R.drawable.edit), null) },
                                value = button2Label,
                                onValueChange = onButton2LabelChange,
                                isInputValid = { true },
                            )

                            ListPreference(
                                title = { Text("Button 2 URL Source") },
                                icon = { Icon(painterResource(R.drawable.link), null) },
                                selectedValue = button2UrlSource,
                                values = DiscordButtonUrlOptions,
                                valueText = { discordUrlSourceLabel(it) },
                                onValueSelected = onButton2UrlSourceChange,
                            )
                        }

                        if (button2Enabled && button2UrlSource == "custom") {
                            EditTextPreference(
                                title = { Text("Button 2 Custom URL") },
                                icon = { Icon(painterResource(R.drawable.link), null) },
                                value = button2CustomUrl,
                                onValueChange = onButton2CustomUrlChange,
                                isInputValid = { true },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun discordUrlSourceLabel(source: String): String =
    when (source) {
        "songurl" -> stringResource(R.string.discord_url_source_song)
        "artisturl" -> stringResource(R.string.discord_url_source_artist)
        "albumurl" -> stringResource(R.string.discord_url_source_album)
        "custom" -> stringResource(R.string.discord_url_source_custom)
        else -> source
    }
