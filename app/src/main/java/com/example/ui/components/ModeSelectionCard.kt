package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PreparationMode
import com.example.ui.theme.*

@Composable
fun ModeSelectionCard(
    mode: PreparationMode,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (mode) {
        PreparationMode.ACADEMIC -> "Academic Studies"
        PreparationMode.PROFESSIONAL_GATE -> "Professional\n(GATE Exam)"
    }
    
    val description = when (mode) {
        PreparationMode.ACADEMIC -> "For school, college & academic exam preparation"
        PreparationMode.PROFESSIONAL_GATE -> "Specialized preparation for GATE aspirants"
    }

    val primaryColor = AppPrimary
    val containerBg = if (isSelected) AppCardContainer else CardBackgroundNeutral
    val borderColor = if (isSelected) primaryColor else BorderNeutral
    val icon = when (mode) {
        PreparationMode.ACADEMIC -> Icons.Default.School
        PreparationMode.PROFESSIONAL_GATE -> Icons.Default.WorkspacePremium
    }
    
    val tag = when (mode) {
        PreparationMode.ACADEMIC -> "mode_card_academic"
        PreparationMode.PROFESSIONAL_GATE -> "mode_card_gate"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag(tag)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) primaryColor else TextMuted,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) primaryColor else TextPrimary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = primaryColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                )
            }
        }
    }
}
