package com.markooverhaul.sunset.v2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.ui.R
import com.markooverhaul.sunset.feature.account.LoginRoute
import com.markooverhaul.sunset.feature.details.DetailsRoute
import com.markooverhaul.sunset.feature.details.DetailsViewModel
import com.markooverhaul.sunset.feature.guide.GuideRoute
import com.markooverhaul.sunset.feature.home.HomeRoute
import com.markooverhaul.sunset.feature.live.LiveRoute
import com.markooverhaul.sunset.feature.live.PpvRoute
import com.markooverhaul.sunset.feature.movies.MoviesRoute
import com.markooverhaul.sunset.feature.mylist.MyListRoute
import com.markooverhaul.sunset.feature.mylist.SavedItem
import com.markooverhaul.sunset.feature.parental.ParentalScreen
import com.markooverhaul.sunset.feature.player.PlayerRoute
import com.markooverhaul.sunset.feature.player.PlayerViewModel
import com.markooverhaul.sunset.feature.profiles.ProfilesScreen
import com.markooverhaul.sunset.feature.search.SearchRoute
import com.markooverhaul.sunset.feature.series.SeriesRoute
import com.markooverhaul.sunset.feature.settings.AboutScreen
import com.markooverhaul.sunset.feature.settings.SettingsRoute
import com.markooverhaul.sunset.v2.BuildConfig
import com.markooverhaul.sunset.v2.ui.MoreScreen
import com.markooverhaul.sunset.v2.update.UpdateMessage
import com.markooverhaul.sunset.v2.update.UpdateViewModel

@Composable
fun SunsetNavHost(
    navController: NavHostController,
    demoMode: Boolean,
    modifier: Modifier = Modifier,
) {
    fun openDetails(type: ContentType, id: String) {
        navController.navigate(SunsetRoutes.details(type, id))
    }

    fun openPlayer(
        type: ContentType,
        id: String,
        title: String,
        episodeId: String? = null,
        episodeTitle: String? = null,
        live: Boolean = false,
    ) {
        navController.navigate(
            SunsetRoutes.player(
                type = type,
                id = id,
                title = title,
                episodeId = episodeId,
                episodeTitle = episodeTitle,
                live = live,
            ),
        )
    }

    fun openChannel(channel: LiveChannel) {
        openPlayer(
            type = ContentType.LIVE,
            id = channel.id,
            title = channel.name,
            live = true,
        )
    }

    NavHost(
        navController = navController,
        startDestination = SunsetRoutes.HOME,
        modifier = modifier,
    ) {
        composable(SunsetRoutes.HOME) {
            HomeRoute(
                onPlayMovie = { movie ->
                    openPlayer(
                        type = ContentType.MOVIE,
                        id = movie.id,
                        title = movie.title,
                    )
                },
                onResume = { progress ->
                    openPlayer(
                        type = progress.contentType,
                        id = progress.contentId,
                        title = progress.title,
                        episodeId = progress.episodeId,
                    )
                },
                onOpenDetails = ::openDetails,
                onOpenChannel = ::openChannel,
                onOpenLive = { navController.navigateTopLevel(SunsetRoutes.LIVE) },
                onOpenMovies = { navController.navigateTopLevel(SunsetRoutes.MOVIES) },
                onOpenSeries = { navController.navigateTopLevel(SunsetRoutes.SERIES) },
                onOpenMyList = { navController.navigateTopLevel(SunsetRoutes.MY_LIST) },
            )
        }

        composable(SunsetRoutes.LIVE) {
            LiveRoute(
                onPlayChannel = ::openChannel,
                onOpenGuide = { navController.navigate(SunsetRoutes.GUIDE) },
            )
        }

        composable(SunsetRoutes.PPV) {
            PpvRoute(onPlayChannel = ::openChannel)
        }

        composable(SunsetRoutes.GUIDE) {
            GuideRoute(
                onPlayChannel = ::openChannel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(SunsetRoutes.MOVIES) {
            MoviesRoute(onOpenDetails = ::openDetails)
        }

        composable(SunsetRoutes.SERIES) {
            SeriesRoute(onOpenDetails = ::openDetails)
        }

        composable(
            route = SunsetRoutes.SEARCH_PATTERN,
            arguments = listOf(
                navArgument("q") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
            deepLinks = listOf(navDeepLink { uriPattern = SunsetRoutes.DEEP_LINK_SEARCH }),
        ) {
            SearchRoute(
                onOpenDetails = ::openDetails,
                onPlayChannel = ::openChannel,
            )
        }

        composable(SunsetRoutes.MY_LIST) {
            MyListRoute(
                onOpen = { item: SavedItem ->
                    if (item.type == ContentType.LIVE) {
                        openPlayer(
                            type = ContentType.LIVE,
                            id = item.id,
                            title = item.title,
                            live = true,
                        )
                    } else {
                        openDetails(item.type, item.id)
                    }
                },
                onOpenMovies = { navController.navigateTopLevel(SunsetRoutes.MOVIES) },
            )
        }

        composable(SunsetRoutes.MORE) {
            MoreScreen(
                onOpenGuide = { navController.navigate(SunsetRoutes.GUIDE) },
                onOpenPpv = { navController.navigateTopLevel(SunsetRoutes.PPV) },
                onOpenSearch = { navController.navigate(SunsetRoutes.search()) },
                onOpenMyList = { navController.navigate(SunsetRoutes.MY_LIST) },
                onOpenSettings = { navController.navigate(SunsetRoutes.SETTINGS) },
            )
        }

        composable(
            route = SunsetRoutes.DETAILS_PATTERN,
            arguments = listOf(
                navArgument(DetailsViewModel.ARG_TYPE) { type = NavType.StringType },
                navArgument(DetailsViewModel.ARG_ID) { type = NavType.StringType },
            ),
            deepLinks = listOf(navDeepLink { uriPattern = SunsetRoutes.DEEP_LINK_DETAILS }),
        ) {
            DetailsRoute(
                onBack = { navController.popBackStack() },
                onPlayMovie = { movie ->
                    openPlayer(
                        type = ContentType.MOVIE,
                        id = movie.id,
                        title = movie.title,
                    )
                },
                onPlayEpisode = { seriesId, seriesTitle, episodeId, episodeTitle ->
                    openPlayer(
                        type = ContentType.SERIES,
                        id = seriesId,
                        title = seriesTitle,
                        episodeId = episodeId,
                        episodeTitle = episodeTitle,
                    )
                },
                onOpenDetails = ::openDetails,
            )
        }

        composable(
            route = SunsetRoutes.PLAYER_PATTERN,
            arguments = listOf(
                navArgument(PlayerViewModel.ARG_TYPE) { type = NavType.StringType },
                navArgument(PlayerViewModel.ARG_ID) { type = NavType.StringType },
                navArgument(PlayerViewModel.ARG_TITLE) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(PlayerViewModel.ARG_EPISODE_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(PlayerViewModel.ARG_EPISODE_TITLE) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(PlayerViewModel.ARG_LIVE) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            PlayerRoute(onExit = { navController.popBackStack() })
        }

        composable(SunsetRoutes.SETTINGS) {
            val updateViewModel: UpdateViewModel = hiltViewModel()
            val updateState by updateViewModel.state.collectAsStateWithLifecycle()
            val updateStatus = when {
                updateState.checking -> stringResource(R.string.update_checking)
                updateState.available != null -> stringResource(R.string.update_available_status)
                updateState.error != null -> stringResource(R.string.update_check_failed)
                updateState.message == UpdateMessage.UpToDate -> stringResource(R.string.update_up_to_date)
                updateState.message == UpdateMessage.DebugBuild -> stringResource(R.string.update_debug_only)
                BuildConfig.DEBUG -> stringResource(R.string.update_debug_only)
                else -> stringResource(
                    R.string.about_version_format,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                )
            }
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(SunsetRoutes.ABOUT) },
                onOpenParental = { navController.navigate(SunsetRoutes.PARENTAL) },
                onOpenProfiles = { navController.navigate(SunsetRoutes.PROFILES) },
                onSignedOut = {
                    navController.navigate(SunsetRoutes.LOGIN) {
                        popUpTo(SunsetRoutes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                demoMode = demoMode,
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                updateStatus = updateStatus,
                onCheckForUpdates = updateViewModel::checkNow,
            )
        }

        composable(SunsetRoutes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                buildType = BuildConfig.BUILD_TYPE,
                demoMode = demoMode,
            )
        }

        composable(SunsetRoutes.PARENTAL) {
            ParentalScreen(onBack = { navController.popBackStack() })
        }

        composable(SunsetRoutes.PROFILES) {
            ProfilesScreen(onBack = { navController.popBackStack() })
        }

        composable(SunsetRoutes.LOGIN) {
            LoginRoute(
                onSignedIn = {
                    navController.navigate(SunsetRoutes.HOME) {
                        popUpTo(SunsetRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                demoMode = demoMode,
            )
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
