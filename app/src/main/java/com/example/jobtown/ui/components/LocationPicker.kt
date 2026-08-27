package com.example.jobtown.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobtown.ui.theme.DeepGreenDark
import com.example.jobtown.ui.theme.SageGreenMain
import com.example.jobtown.ui.theme.TextDark
import com.example.jobtown.utils.LocationOptions
import com.example.jobtown.utils.ValidationUtils

/**
 * Country (dropdown) + City (free text) pair, with an optional "add another branch"
 * flow so employers with more than one office location can list all of them.
 * The combined result is reported through [onLocationStringChange] as a single
 * "City, Country | City, Country" string (see [LocationOptions]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPicker(
    locationString: String,
    onLocationStringChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Location",
    allowMultipleBranches: Boolean = false,
    errorText: String? = null
) {
    val parsed = remember(locationString) { LocationOptions.parseAddresses(locationString) }

    var country by remember(locationString) { mutableStateOf(parsed.getOrNull(0)?.country.orEmpty()) }
    var city by remember(locationString) { mutableStateOf(parsed.getOrNull(0)?.city.orEmpty()) }
    var branches by remember(locationString) {
        mutableStateOf(parsed.drop(1).map { it.city to it.country })
    }
    var expanded by remember { mutableStateOf(false) }

    fun emit() {
        val primary = LocationOptions.Address(city = city.trim(), country = country.trim())
        val branchAddresses = branches.map { (c, co) -> LocationOptions.Address(city = c.trim(), country = co.trim()) }
        onLocationStringChange(LocationOptions.buildLocationString(primary, branchAddresses))
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = country,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Country") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    isError = errorText != null,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    LocationOptions.COUNTRIES.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                country = option
                                expanded = false
                                emit()
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = city,
                onValueChange = {
                    city = it.take(ValidationUtils.LOCATION_MAX_LENGTH)
                    emit()
                },
                label = { Text("City") },
                isError = errorText != null,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        if (errorText != null) {
            Text(text = errorText, color = Color.Red, fontSize = 12.sp)
        }

        if (allowMultipleBranches) {
            branches.forEachIndexed { index, (branchCity, branchCountry) ->
                BranchRow(
                    city = branchCity,
                    country = branchCountry,
                    onCityChange = { newCity ->
                        branches = branches.toMutableList().also { it[index] = newCity to branchCountry }
                        emit()
                    },
                    onCountryChange = { newCountry ->
                        branches = branches.toMutableList().also { it[index] = branchCity to newCountry }
                        emit()
                    },
                    onRemove = {
                        branches = branches.toMutableList().also { it.removeAt(index) }
                        emit()
                    }
                )
            }

            TextButton(onClick = { branches = branches + ("" to "") }) {
                Icon(Icons.Default.Add, contentDescription = null, tint = DeepGreenDark, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add another branch / office", color = DeepGreenDark, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BranchRow(
    city: String,
    country: String,
    onCityChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = SageGreenMain, modifier = Modifier.size(18.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = country,
                onValueChange = {},
                readOnly = true,
                label = { Text("Country") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                LocationOptions.COUNTRIES.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onCountryChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = city,
            onValueChange = { onCityChange(it.take(ValidationUtils.LOCATION_MAX_LENGTH)) },
            label = { Text("City") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
        )

        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove branch", tint = TextDark.copy(alpha = 0.5f))
        }
    }
}
