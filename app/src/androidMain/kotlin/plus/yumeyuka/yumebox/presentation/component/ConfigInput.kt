/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

package plus.yumeyuka.yumebox.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.StringListEditorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.KeyValueEditorScreenDestination
import plus.yumeyuka.yumebox.presentation.screen.EditorDataHolder
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import dev.oom_wg.purejoy.mlang.MLang

@Composable
fun PortInput(
    title: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value?.toString() ?: "") }
    var showDialog by remember { mutableStateOf(false) }

    SuperArrow(
        title = title,
        summary = if (value != null) "$value" else MLang.Component.Selector.NotModify,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        SuperBottomSheet(
            show = remember { mutableStateOf(true) },
            title = title,
            onDismissRequest = { showDialog = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it.filter { c -> c.isDigit() } },
                    label = MLang.Component.ConfigInput.PortLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                DialogButtonRow(
                    onCancel = {
                        onValueChange(null)
                        showDialog = false
                    },
                    onConfirm = {
                        val port = textValue.toIntOrNull()
                        if (port == null || (port in 1..65535)) {
                            onValueChange(port)
                        }
                        showDialog = false
                    },
                    cancelText = MLang.Component.Button.Clear,
                )
            }
        }
    }
}

@Composable
fun StringInput(
    title: String,
    value: String?,
    placeholder: String = "",
    onValueChange: (String?) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value ?: "") }
    var showDialog by remember { mutableStateOf(false) }

    SuperArrow(
        title = title,
        summary = value?.takeIf { it.isNotEmpty() } ?: MLang.Component.Selector.NotModify,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        SuperBottomSheet(
            show = remember { mutableStateOf(true) },
            title = title,
            onDismissRequest = { showDialog = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = placeholder,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                DialogButtonRow(
                    onCancel = {
                        onValueChange(null)
                        showDialog = false
                    },
                    onConfirm = {
                        onValueChange(textValue.takeIf { it.isNotEmpty() })
                        showDialog = false
                    },
                    cancelText = MLang.Component.Button.Clear,
                )
            }
        }
    }
}

@Composable
fun StringListInput(
    title: String,
    value: List<String>?,
    placeholder: String = "",
    navigator: DestinationsNavigator,
    onValueChange: (List<String>?) -> Unit,
) {
    val itemCount = value?.size ?: 0
    val displayValue = if (itemCount > 0) MLang.Component.ConfigInput.CountItems.format(itemCount) else MLang.Component.Selector.NotModify

    SuperArrow(
        title = title,
        summary = displayValue,
        onClick = {
            EditorDataHolder.setupListEditor(
                title = title,
                placeholder = placeholder,
                items = value,
                callback = onValueChange,
            )
            navigator.navigate(StringListEditorScreenDestination)
        },
    )
}

@Composable
fun StringMapInput(
    title: String,
    value: Map<String, String>?,
    keyPlaceholder: String = MLang.Component.ConfigInput.KeyPlaceholder,
    valuePlaceholder: String = MLang.Component.ConfigInput.ValuePlaceholder,
    navigator: DestinationsNavigator,
    onValueChange: (Map<String, String>?) -> Unit,
) {
    val itemCount = value?.size ?: 0
    val displayValue = if (itemCount > 0) MLang.Component.ConfigInput.CountItems.format(itemCount) else MLang.Component.Selector.NotModify

    SuperArrow(
        title = title,
        summary = displayValue,
        onClick = {
            EditorDataHolder.setupMapEditor(
                title = title,
                keyPlaceholder = keyPlaceholder,
                valuePlaceholder = valuePlaceholder,
                items = value,
                callback = onValueChange,
            )
            navigator.navigate(KeyValueEditorScreenDestination)
        },
    )
}
