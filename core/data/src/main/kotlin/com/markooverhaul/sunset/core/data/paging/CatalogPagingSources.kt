package com.markooverhaul.sunset.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.SeriesSummary
import kotlinx.coroutines.CancellationException

/** Paging 3 bridge over [ContentProvider.getMovies]. The provider owns page size and totals. */
class MoviePagingSource(
    private val provider: ContentProvider,
    private val categoryId: String?,
) : PagingSource<Int, Movie>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: FIRST_PAGE
        return try {
            val result = provider.getMovies(categoryId = categoryId, page = page)
            LoadResult.Page(
                data = result.items,
                prevKey = if (page <= FIRST_PAGE) null else page - 1,
                nextKey = if (result.hasMore) page + 1 else null,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }

    private companion object {
        const val FIRST_PAGE = 1
    }
}

/** Paging 3 bridge over [ContentProvider.getSeries]. */
class SeriesPagingSource(
    private val provider: ContentProvider,
    private val categoryId: String?,
) : PagingSource<Int, SeriesSummary>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SeriesSummary> {
        val page = params.key ?: FIRST_PAGE
        return try {
            val result = provider.getSeries(categoryId = categoryId, page = page)
            LoadResult.Page(
                data = result.items,
                prevKey = if (page <= FIRST_PAGE) null else page - 1,
                nextKey = if (result.hasMore) page + 1 else null,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SeriesSummary>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
