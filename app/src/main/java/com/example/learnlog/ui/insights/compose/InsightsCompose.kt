package com.example.learnlog.ui.insights.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnlog.R
import com.example.learnlog.data.repository.CachedQuote
import kotlinx.coroutines.flow.StateFlow

/** Plus Jakarta Sans for Compose, matching the XML theme. */
private val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold)
)

/** Quote-of-the-day card on the Insights screen (Jetpack Compose inside the XML layout). */
@Composable
fun QuoteOfTheDayCard(quoteFlow: StateFlow<CachedQuote?>, modifier: Modifier = Modifier) {
    val quote by quoteFlow.collectAsState()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.ll_secondary_container)),
        border = BorderStroke(0.dp, colorResource(R.color.ll_secondary_container))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_quote),
                    contentDescription = null,
                    tint = colorResource(R.color.ll_secondary),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.quote_of_the_day),
                    style = TextStyle(
                        fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        color = colorResource(R.color.ll_on_secondary_container)
                    )
                )
            }
            AnimatedContent(
                targetState = quote,
                transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(200)) },
                label = "quote"
            ) { q ->
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = q?.content?.let { "“$it”" } ?: "",
                        style = TextStyle(
                            fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 17.sp,
                            lineHeight = 25.sp, color = colorResource(R.color.ll_on_secondary_container)
                        )
                    )
                    if (q != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "— ${q.author}",
                            style = TextStyle(
                                fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                color = colorResource(R.color.ll_secondary)
                            )
                        )
                    }
                }
            }
        }
    }
}
