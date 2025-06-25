package ng.mona.paywithmona.presentation.bottomsheet

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.mona.paywithmona.R
import ng.mona.paywithmona.data.model.Collection
import ng.mona.paywithmona.data.model.CollectionSchedule
import ng.mona.paywithmona.data.model.CollectionScheduleEntry
import ng.mona.paywithmona.data.model.CollectionType
import ng.mona.paywithmona.presentation.shared.ExpandHeader
import ng.mona.paywithmona.presentation.theme.SdkColors
import ng.mona.paywithmona.presentation.theme.SdkTheme
import ng.mona.paywithmona.util.capitalize
import ng.mona.paywithmona.util.format
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class CollectionDetailsGridState {
    Collapsed,
    AutoExpanded,
    Default
}

@Composable
internal fun CollectionDetailsGrid(
    merchantName: String,
    collection: Collection,
    modifier: Modifier = Modifier,
    state: CollectionDetailsGridState = CollectionDetailsGridState.Default
) {
    val isScheduled = remember(collection.schedule?.type) {
        collection.schedule?.type?.equals(CollectionType.Scheduled.name, true) == true
    }
    var scheduleExpanded by remember(isScheduled, state) {
        mutableStateOf(state == CollectionDetailsGridState.AutoExpanded)
    }

    val entries = remember(merchantName, collection, state) {
        val result = buildList {
            add(Triple(R.drawable.ic_person, "Debitor", merchantName))
            add(
                Triple(
                    R.drawable.ic_calendar,
                    if (isScheduled) "Duration" else "Frequency",
                    if (isScheduled) formatDate(collection.expiryDate) else collection.schedule?.frequency?.capitalize(
                        true
                    )
                        ?: "-"
                )
            )
            add(
                Triple(
                    R.drawable.ic_money,
                    if (isScheduled) "Total debit limit" else "Amount",
                    collection.maxAmountInKobo?.toIntOrNull()?.format() ?: "-"
                )
            )
            add(
                Triple(
                    if (isScheduled) R.drawable.ic_money else R.drawable.ic_calendar,
                    if (isScheduled) "Monthly debit limit" else "Start",
                    when (isScheduled) {
                        true -> collection.monthlyLimit?.toIntOrNull()?.format() ?: "-"
                        false -> formatDate(collection.startDate)
                    }
                )
            )
            collection.reference?.let {
                add(Triple(R.drawable.ic_files, "Reference", it))
            }
        }.chunked(2)
        when (state) {
            CollectionDetailsGridState.Collapsed -> result.take(1)
            else -> result
        }
    }

    Column(
        modifier = modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = {
            entries.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = {
                        row.forEach { (icon, label, value) ->
                            DetailItem(
                                icon = icon,
                                label = label,
                                value = value,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                )
            }
            if (isScheduled && state != CollectionDetailsGridState.Collapsed && !collection.schedule?.entries.isNullOrEmpty()) {
                if (scheduleExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SdkColors.divider, RoundedCornerShape(4.dp)),
                        content = {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                content = {
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = "Date",
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.W500,
                                        fontSize = 14.sp,
                                        color = SdkColors.darkText,
                                    )
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = "Amount",
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.W500,
                                        fontSize = 14.sp,
                                        color = SdkColors.darkText,
                                    )
                                }
                            )
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 320.dp),
                                content = {
                                    itemsIndexed(collection.schedule.entries) { index, entry ->
                                        ScheduleItem(
                                            date = entry.date,
                                            amount = entry.amountInKobo,
                                        )
                                    }
                                }
                            )
                        }
                    )
                }
                if (state != CollectionDetailsGridState.AutoExpanded) {
                    ExpandHeader(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Show repayments",
                        expanded = scheduleExpanded,
                        onToggle = {
                            scheduleExpanded = !scheduleExpanded
                        }
                    )
                }
            }
        }
    )
}


@Composable
private fun DetailItem(
    modifier: Modifier = Modifier,
    icon: Int,
    label: String,
    value: String
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
                content = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = icon),
                        contentDescription = label,
                        tint = SdkColors.darkText,
                    )
                }
            )
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W300,
                    color = SdkColors.subText
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500,
                    color = SdkColors.darkText,
                    lineHeight = 20.sp
                )
            }
        }
    )
}

@Composable
private fun ScheduleItem(
    date: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Text(
                modifier = Modifier.weight(1f),
                text = formatDate(date, true),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = SdkColors.darkText,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = amount.toIntOrNull()?.format().orEmpty(),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = SdkColors.subText,
            )
        }
    )
}


private fun formatDate(iso: String?, schedule: Boolean = false): String {
    if (iso.isNullOrBlank()) return "-"
    return try {
        val zonedDateTime = ZonedDateTime.parse(iso)
        val formatter = DateTimeFormatter.ofPattern(
            if (schedule) "dd/MM/yyyy" else "d MMM y",
            Locale.getDefault()
        )
        formatter.format(zonedDateTime)
    } catch (_: Exception) {
        iso
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionDetailsGridPreview() = SdkTheme {
    CollectionDetailsGrid(
        merchantName = "John Doe",
        collection = Collection(
            id = "12345",
            maxAmountInKobo = "100000",
            startDate = "2023-01-01T00:00:00Z",
            expiryDate = "2024-01-01T00:00:00Z",
            schedule = null,
            reference = "REF12345"
        ),
        modifier = Modifier.fillMaxWidth()
    )
}


@Preview(showBackground = true)
@Composable
private fun CollectionWithScheduleDetailsGridPreview() = SdkTheme {
    CollectionDetailsGrid(
        merchantName = "John Doe",
        collection = Collection(
            id = "12345",
            maxAmountInKobo = "100000",
            startDate = "2023-01-01T00:00:00Z",
            expiryDate = "2024-01-01T00:00:00Z",
            schedule = CollectionSchedule(
                type = CollectionType.Scheduled.name,
                entries = listOf(
                    CollectionScheduleEntry(amountInKobo = "10000", date = "2023-01-01T00:00:00Z"),
                    CollectionScheduleEntry(amountInKobo = "20000", date = "2023-02-01T00:00:00Z")
                ),
                frequency = "Monthly"
            ),
            reference = "REF12345"
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
private fun CollapsedCollectionScheduleDetailsGridPreview() = SdkTheme {
    CollectionDetailsGrid(
        modifier = Modifier.fillMaxWidth(),
        merchantName = "John Doe",
        collection = Collection(
            id = "12345",
            maxAmountInKobo = "100000",
            startDate = "2023-01-01T00:00:00Z",
            expiryDate = "2024-01-01T00:00:00Z",
            reference = "REF12345"
        ),
        state = CollectionDetailsGridState.Collapsed
    )
}