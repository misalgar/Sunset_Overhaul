package com.markooverhaul.sunset.v2.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.markooverhaul.sunset.core.designsystem.components.SunsetFocusable
import com.markooverhaul.sunset.core.designsystem.components.SunsetWordmark
import com.markooverhaul.sunset.core.designsystem.tokens.LocalIsTelevision
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetRadius
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.v2.navigation.SunsetNavHost
import com.markooverhaul.sunset.v2.navigation.SunsetRoutes

private const val LARGE_SCREEN_DP = 840

private data class TopNavItem(
    val route: String,
    @StringRes val label: Int,
)

private data class BottomNavItem(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val LargeNavItems = listOf(
    TopNavItem(SunsetRoutes.HOME, R.string.nav_home),
    TopNavItem(SunsetRoutes.LIVE, R.string.nav_live),
    TopNavItem(SunsetRoutes.PPV, R.string.nav_ppv),
    TopNavItem(SunsetRoutes.MOVIES, R.string.nav_movies),
    TopNavItem(SunsetRoutes.SERIES, R.string.nav_series),
    TopNavItem(SunsetRoutes.SEARCH_PATTERN, R.string.nav_search),
    TopNavItem(SunsetRoutes.MY_LIST, R.string.nav_my_list),
    TopNavItem(SunsetRoutes.MORE, R.string.nav_more),
)

private val BottomNavItems = listOf(
    BottomNavItem(SunsetRoutes.HOME, R.string.nav_home, Icons.Rounded.Home),
    BottomNavItem(SunsetRoutes.LIVE, R.string.nav_live, Icons.Rounded.LiveTv),
    BottomNavItem(SunsetRoutes.MOVIES, R.string.nav_movies, Icons.Rounded.Movie),
    BottomNavItem(SunsetRoutes.SERIES, R.string.nav_series, Icons.Rounded.Tv),
    BottomNavItem(SunsetRoutes.MORE, R.string.nav_more, Icons.Rounded.Menu),
)

private val PhoneTopLevel = setOf(
    SunsetRoutes.HOME,
    SunsetRoutes.LIVE,
    SunsetRoutes.MOVIES,
    SunsetRoutes.SERIES,
    SunsetRoutes.MORE,
)

private val LargeTopLevel = setOf(
    SunsetRoutes.HOME,
    SunsetRoutes.LIVE,
    SunsetRoutes.PPV,
    SunsetRoutes.MOVIES,
    SunsetRoutes.SERIES,
    SunsetRoutes.SEARCH_PATTERN,
    SunsetRoutes.MY_LIST,
    SunsetRoutes.MORE,
)

/**
 * Adaptive shell: bottom navigation on phones, a wordmark + top navigation bar on tablets and
 * television. Detail, player and login routes hide the shell chrome so they can own the screen.
 */
@Composable
fun MainShell(
    demoMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: SunsetRoutes.HOME
    val isTv = LocalIsTelevision.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val isLargeScreen = isTv || windowWidthDp >= LARGE_SCREEN_DP.dp

    val showTopBar = isLargeScreen && currentRoute in LargeTopLevel
    val showBottomBar = !isLargeScreen && currentRoute in PhoneTopLevel

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SunsetColors.BgBase,
        topBar = {
            if (showTopBar) {
                LargeScreenTopBar(
                    isTv = isTv,
                    currentRoute = currentRoute,
                    onNavigate = { route -> navController.navigateTopLevel(route) },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                PhoneBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route -> navController.navigateTopLevel(route) },
                )
            }
        },
    ) { padding ->
        SunsetNavHost(
            navController = navController,
            demoMode = demoMode,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun LargeScreenTopBar(
    isTv: Boolean,
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
    val gutter = if (isTv) 48.dp else 24.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isTv) 88.dp else 72.dp)
            .background(SunsetColors.BgBase)
            .padding(horizontal = gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SunsetWordmark(symbolSize = if (isTv) 32.dp else 28.dp)
        Spacer(modifier = Modifier.width(if (isTv) 32.dp else 20.dp))
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(LargeNavItems, key = { it.route }) { item ->
                TopNavEntry(
                    label = stringResource(item.label),
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                )
            }
        }
    }
}

@Composable
private fun TopNavEntry(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(SunsetRadius.Pill)
    SunsetFocusable(
        onClick = onClick,
        shape = shape,
        scaleOnFocus = false,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .background(
                    color = if (selected) SunsetColors.BgSurface2 else SunsetColors.BgBase,
                    shape = shape,
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) SunsetColors.AccentPrimary else SunsetColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun PhoneBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = SunsetColors.BgSurface,
        tonalElevation = 0.dp,
    ) {
        BottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.label),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SunsetColors.AccentPrimary,
                    selectedTextColor = SunsetColors.AccentPrimary,
                    indicatorColor = SunsetColors.BgSurface3,
                    unselectedIconColor = SunsetColors.TextSecondary,
                    unselectedTextColor = SunsetColors.TextSecondary,
                ),
            )
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    val target = if (route == SunsetRoutes.SEARCH_PATTERN) SunsetRoutes.search() else route
    navigate(target) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
