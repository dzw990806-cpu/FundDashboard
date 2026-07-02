package com.example.funddashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FundDashboardApp(context = this)
            }
        }
    }
}

@Composable
fun FundDashboardApp(context: Context) {
    var selectedTab by remember { mutableStateOf(0) }
    var funds by remember { mutableStateOf(mockFunds()) }
    var showDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            funds = refreshFunds(funds)
        }
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("自选") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    label = { Text("持仓") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> FundListScreen(
                    funds = funds,
                    onRefresh = { funds = refreshFunds(funds) },
                    onCopy = { copyFundData(context, it) },
                    onAdd = { showDialog = true }
                )
                1 -> PortfolioScreen(funds = funds)
            }
        }
    }
    
    if (showDialog) {
        AddFundDialog(
            onDismiss = { showDialog = false },
            onAdd = { name, code ->
                funds = addNewFund(funds, name, code)
                showDialog = false
            }
        )
    }
}

@Composable
fun FundListScreen(
    funds: List<Fund>,
    onRefresh: () -> Unit,
    onCopy: (Fund) -> Unit,
    onAdd: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自选基金") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(funds) { fund ->
                FundCard(fund = fund, onCopy = { onCopy(fund) })
            }
        }
    }
}

@Composable
fun PortfolioScreen(funds: List<Fund>) {
    val totalAmount = funds.sumOf { it.nav * 1000 }
    val totalReturn = funds.sumOf { it.historicalReturns.oneYear }
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("持仓收益") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A237E)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("总资产", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text("¥${String.format("%.0f", totalAmount)}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("昨日收益", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text("+${String.format("%.1f", totalReturn)}%", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(funds) { fund ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(fund.name, fontWeight = FontWeight.Medium)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("¥${String.format("%.0f", fund.nav * 1000)}")
                                Text(
                                    "+${String.format("%.1f", fund.historicalReturns.oneWeek)}%",
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FundCard(fund: Fund, onCopy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(fund.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(fund.code, fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("净值", fontSize = 12.sp, color = Color.Gray)
                    Text(String.format("%.4f", fund.nav), fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("指数", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        String.format("%.2f", fund.trackingIndex),
                        color = if (fund.trackingIndex >= 0) Color(0xFF4CAF50) else Color.Red
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("近一周", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "+${String.format("%.1f", fund.historicalReturns.oneWeek)}%",
                        color = Color(0xFF4CAF50)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("近一月", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "+${String.format("%.1f", fund.historicalReturns.oneMonth)}%",
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
fun AddFundDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加基金") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("基金名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("基金代码") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank() && code.isNotBlank()) onAdd(name, code) }) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

data class Fund(
    val id: String,
    val code: String,
    val name: String,
    val nav: Double,
    val trackingIndex: Double,
    val historicalReturns: HistoricalReturns
)

data class HistoricalReturns(
    val oneWeek: Double,
    val oneMonth: Double,
    val threeMonths: Double,
    val oneYear: Double
)

fun mockFunds(): List<Fund> = listOf(
    Fund(
        id = "1",
        code = "110011",
        name = "易方达中小盘混合",
        nav = 4.5230,
        trackingIndex = 1.23,
        historicalReturns = HistoricalReturns(2.5, 5.8, 12.3, 35.2)
    ),
    Fund(
        id = "2",
        code = "000083",
        name = "汇添富消费行业混合",
        nav = 6.2150,
        trackingIndex = 0.87,
        historicalReturns = HistoricalReturns(1.8, 4.2, 9.8, 28.9)
    ),
    Fund(
        id = "3",
        code = "001678",
        name = "中欧医疗健康混合A",
        nav = 3.8760,
        trackingIndex = -0.45,
        historicalReturns = HistoricalReturns(-1.2, 3.6, 8.5, 25.3)
    )
)

fun refreshFunds(funds: List<Fund>): List<Fund> {
    return funds.map { fund ->
        fund.copy(
            nav = fund.nav * (1 + Random.nextDouble(-0.01, 0.01)),
            trackingIndex = Random.nextDouble(-2.0, 2.0)
        )
    }
}

fun addNewFund(funds: List<Fund>, name: String, code: String): List<Fund> {
    val newFund = Fund(
        id = System.currentTimeMillis().toString(),
        code = code,
        name = name,
        nav = Random.nextDouble(1.0, 10.0),
        trackingIndex = Random.nextDouble(-2.0, 2.0),
        historicalReturns = HistoricalReturns(
            oneWeek = Random.nextDouble(-5.0, 10.0),
            oneMonth = Random.nextDouble(-5.0, 15.0),
            threeMonths = Random.nextDouble(-5.0, 20.0),
            oneYear = Random.nextDouble(-5.0, 35.0)
        )
    )
    return funds + newFund
}

fun copyFundData(context: Context, fund: Fund) {
    val text = "基金名称: ${fund.name} + 基金代码: ${fund.code} + 净值: ${String.format("%.4f", fund.nav)} + " +
            "跟踪指数: ${String.format("%.2f", fund.trackingIndex)} + " +
            "近一周: +${String.format("%.1f", fund.historicalReturns.oneWeek)}% + " +
            "近一月: +${String.format("%.1f", fund.historicalReturns.oneMonth)}% + " +
            "近三月: +${String.format("%.1f", fund.historicalReturns.threeMonths)}% + " +
            "近1年: +${String.format("%.1f", fund.historicalReturns.oneYear)}%"
    
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Fund Data", text))
}
