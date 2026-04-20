package com.poke86.game.ui.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.poke86.game.domain.model.Category
import com.poke86.game.ui.theme.GameVaultTheme

@Composable
fun CategoryChips(
    categories: List<Category>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category.id == selectedCategoryId,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.label) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryChipsPreview() {
    GameVaultTheme {
        CategoryChips(
            categories = listOf(
                Category("all", "전체"),
                Category("party", "파티"),
                Category("solo", "혼자"),
                Category("reflex", "반응속도"),
                Category("brain", "두뇌")
            ),
            selectedCategoryId = "all",
            onCategorySelected = {}
        )
    }
}
