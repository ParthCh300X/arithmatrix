package udemy.appdev.arithmatrix.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

val WIDGET_LAST_RESULT_KEY = stringPreferencesKey("widget_last_result")
val WIDGET_LAST_EXPR_KEY   = stringPreferencesKey("widget_last_expr")

class ArithMatrixWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }
}

@Composable
fun WidgetContent() {
    val prefs  = currentState<Preferences>()
    val expr   = prefs[WIDGET_LAST_EXPR_KEY]   ?: "ArithMatrix"
    val result = prefs[WIDGET_LAST_RESULT_KEY] ?: "Open app to calculate"

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(ColorProvider(Color(0xFF1E1E2E)))
            .clickable(actionRunCallback<OpenAppAction>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = expr,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFAAAAAA)),
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
            Text(
                text = "= $result",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF7EC8A4)),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
        Text(
            text = "calc →",
            style = TextStyle(
                color = ColorProvider(Color(0xFF7EC8A4)),
                fontSize = 11.sp
            )
        )
    }
}

class OpenAppAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        if (intent != null) context.startActivity(intent)
    }
}

class ArithMatrixWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ArithMatrixWidget()
}

suspend fun updateWidget(
    context: Context,
    expression: String,
    result: String
) {
    val manager = GlanceAppWidgetManager(context)

    val glanceIds =
        manager.getGlanceIds(ArithMatrixWidget::class.java)

    glanceIds.forEach { glanceId ->

        updateAppWidgetState(
            context = context,
            glanceId = glanceId
        ) { prefs: MutablePreferences ->
            prefs[WIDGET_LAST_EXPR_KEY] = expression
            prefs[WIDGET_LAST_RESULT_KEY] = result
        }

        ArithMatrixWidget().update(
            context = context,
            id = glanceId
        )
    }
}