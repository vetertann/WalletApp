package com.ivan.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivan.wallet.data.model.Category
import com.ivan.wallet.data.model.CategoryGroup

/**
 * Hierarchical category picker. Renders collapsible group headers; tapping a group
 * expands a list of leaf radios. Used in both the entry dialog and the budget dialog.
 *
 * @param selected currently picked category (null = no selection)
 * @param onSelect invoked when the user picks a leaf or clears (null when [allowNone])
 * @param visibleGroups which groups to show. Defaults to all.
 * @param excluded leaves to hide entirely (e.g. categories that already have a budget).
 * @param allowNone show a "Use learned rule or leave uncategorized" option at the top.
 * @param noneLabel label shown next to the "no category" radio.
 */
@Composable
fun CategoryPicker(
    selected: Category?,
    onSelect: (Category?) -> Unit,
    visibleGroups: List<CategoryGroup> = CategoryGroup.entries.toList(),
    excluded: Set<Category> = emptySet(),
    allowNone: Boolean = false,
    noneLabel: String = "Use learned rule or leave uncategorized"
) {
    var expandedGroup by remember(visibleGroups) {
        mutableStateOf(selected?.group ?: visibleGroups.firstOrNull())
    }
    LaunchedEffect(selected) {
        if (selected != null) expandedGroup = selected.group
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (allowNone) {
            CategoryRadio(
                label = noneLabel,
                accent = Color(0xFFB4B2A9),
                selected = selected == null,
                onClick = { onSelect(null) }
            )
        }
        visibleGroups.forEach { group ->
            val groupLeaves = Category.entries.filter { it.group == group && it !in excluded }
            if (groupLeaves.isEmpty()) return@forEach
            val expanded = expandedGroup == group
            GroupHeader(
                group = group,
                expanded = expanded,
                onClick = { expandedGroup = if (expanded) null else group }
            )
            if (expanded) {
                groupLeaves.forEach { cat ->
                    CategoryRadio(
                        label = cat.label,
                        accent = cat.accent,
                        selected = selected == cat,
                        onClick = { onSelect(cat) },
                        indented = true
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(
    group: CategoryGroup,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(group.accent)
            )
            Text(
                text = group.label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        Text(
            text = if (expanded) "▾" else "›",
            color = Color(0xFF7A736A),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun CategoryRadio(
    label: String,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    indented: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = if (indented) 18.dp else 0.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Text(text = label, fontSize = 13.sp)
    }
}
